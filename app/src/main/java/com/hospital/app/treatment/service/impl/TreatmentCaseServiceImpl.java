package com.hospital.app.treatment.service.impl;

import com.hospital.app.common.enums.CaseStatus;
import com.hospital.app.exception.ResourceNotFoundException;
import com.hospital.app.patient.entity.Patient;
import com.hospital.app.patient.repository.PatientRepository;
import com.hospital.app.treatment.dto.request.CreateTreatmentCaseRequest;
import com.hospital.app.treatment.dto.response.TreatmentCaseResponse;
import com.hospital.app.treatment.entity.TreatmentCase;
import com.hospital.app.treatment.exception.InvalidCaseStateException;
import com.hospital.app.treatment.exception.TreatmentNotFoundException;
import com.hospital.app.treatment.mapper.TreatmentCaseMapper;
import com.hospital.app.treatment.repository.TreatmentCaseRepository;
import com.hospital.app.treatment.service.TreatmentCaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TreatmentCaseServiceImpl implements TreatmentCaseService {

    private final TreatmentCaseRepository treatmentCaseRepository;
    private final PatientRepository patientRepository;
    private final TreatmentCaseMapper treatmentCaseMapper;

    @Override
    @Transactional
    public TreatmentCaseResponse createTreatmentCase(CreateTreatmentCaseRequest request) {
        log.info("Creating treatment case for patient: {}", request.patientId());

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + request.patientId()));

        TreatmentCase treatmentCase = TreatmentCase.builder()
                .patient(patient)
                .title(request.title())
                .diagnosis(request.diagnosis())
                .caseType(request.caseType())
                .status(CaseStatus.ACTIVE)
                .openDate(LocalDateTime.now())
                .build();

        TreatmentCase saved = treatmentCaseRepository.save(treatmentCase);
        log.info("Created treatment case with ID: {}", saved.getTreatmentCaseId());

        return treatmentCaseMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TreatmentCaseResponse getTreatmentCaseById(UUID caseId) {
        TreatmentCase treatmentCase = treatmentCaseRepository.findById(caseId)
                .orElseThrow(() -> new TreatmentNotFoundException("Treatment Case not found with ID: " + caseId));
        return treatmentCaseMapper.toResponse(treatmentCase);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentCaseResponse> getTreatmentCasesForPatient(UUID patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found with ID: " + patientId);
        }

        return treatmentCaseRepository.findByPatientPatientIdOrderByOpenDateDesc(patientId)
                .stream()
                .map(treatmentCaseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TreatmentCaseResponse closeTreatmentCase(UUID caseId) {
        log.info("Closing treatment case: {}", caseId);

        TreatmentCase treatmentCase = treatmentCaseRepository.findById(caseId)
                .orElseThrow(() -> new TreatmentNotFoundException("Treatment Case not found with ID: " + caseId));

        if (treatmentCase.getStatus() == CaseStatus.CLOSED) {
            throw new InvalidCaseStateException("Treatment Case is already closed");
        }

        treatmentCase.setStatus(CaseStatus.CLOSED);
        treatmentCase.setCloseDate(LocalDateTime.now());
        
        TreatmentCase saved = treatmentCaseRepository.save(treatmentCase);
        log.info("Closed treatment case with ID: {}", saved.getTreatmentCaseId());

        return treatmentCaseMapper.toResponse(saved);
    }
}
