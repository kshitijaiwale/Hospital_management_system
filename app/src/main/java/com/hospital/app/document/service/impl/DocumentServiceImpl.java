package com.hospital.app.document.service.impl;

import com.hospital.app.document.dto.request.UploadDocumentRequest;
import com.hospital.app.document.dto.response.DocumentResponse;
import com.hospital.app.document.entity.PatientDocument;
import com.hospital.app.document.mapper.DocumentMapper;
import com.hospital.app.document.repository.DocumentRepository;
import com.hospital.app.document.service.DocumentService;
import com.hospital.app.document.service.FileStorageService;
import com.hospital.app.exception.ResourceNotFoundException;
import com.hospital.app.patient.entity.Patient;
import com.hospital.app.patient.repository.PatientRepository;
import com.hospital.app.security.entity.User;
import com.hospital.app.security.repository.UserRepository;
import com.hospital.app.treatment.entity.Consultation;
import com.hospital.app.treatment.entity.TreatmentCase;
import com.hospital.app.treatment.repository.ConsultationRepository;
import com.hospital.app.treatment.repository.TreatmentCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final PatientRepository patientRepository;
    private final TreatmentCaseRepository treatmentCaseRepository;
    private final ConsultationRepository consultationRepository;
    private final UserRepository userRepository;
    private final DocumentMapper documentMapper;

    @Override
    @Transactional
    public DocumentResponse uploadDocument(UploadDocumentRequest request) {
        log.info("Uploading document for patient: {}", request.getPatientId());

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        TreatmentCase treatmentCase = null;
        if (request.getTreatmentCaseId() != null) {
            treatmentCase = treatmentCaseRepository.findById(request.getTreatmentCaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Treatment Case not found"));
        }

        Consultation consultation = null;
        if (request.getConsultationId() != null) {
            consultation = consultationRepository.findById(request.getConsultationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Consultation not found"));
        }

        User uploader = resolveCurrentUser();
        if (uploader == null) {
            throw new IllegalStateException("Current user could not be resolved");
        }

        String storedFileName = fileStorageService.storeFile(request.getFile());

        PatientDocument document = PatientDocument.builder()
                .patient(patient)
                .treatmentCase(treatmentCase)
                .consultation(consultation)
                .documentType(request.getDocumentType())
                .fileName(request.getFile().getOriginalFilename())
                .filePath(storedFileName) // Store the generated unique filename to retrieve it
                .uploadedBy(uploader)
                .build();

        PatientDocument savedDocument = documentRepository.save(document);
        log.info("Document successfully saved with ID: {}", savedDocument.getDocumentId());

        DocumentResponse response = documentMapper.toResponse(savedDocument);
        response.setDownloadUrl("/api/v1/documents/" + savedDocument.getDocumentId() + "/download");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getPatientDocuments(UUID patientId) {
        return documentRepository.findByPatientPatientIdOrderByUploadedAtDesc(patientId)
                .stream()
                .map(doc -> {
                    DocumentResponse res = documentMapper.toResponse(doc);
                    res.setDownloadUrl("/api/v1/documents/" + doc.getDocumentId() + "/download");
                    return res;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(UUID documentId) {
        PatientDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        
        DocumentResponse response = documentMapper.toResponse(document);
        response.setDownloadUrl("/api/v1/documents/" + document.getDocumentId() + "/download");
        return response;
    }

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }
}
