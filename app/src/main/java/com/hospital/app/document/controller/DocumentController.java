package com.hospital.app.document.controller;

import com.hospital.app.document.dto.request.UploadDocumentRequest;
import com.hospital.app.document.dto.response.DocumentResponse;
import com.hospital.app.document.entity.PatientDocument;
import com.hospital.app.document.repository.DocumentRepository;
import com.hospital.app.document.service.DocumentService;
import com.hospital.app.document.service.FileStorageService;
import com.hospital.app.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<DocumentResponse> uploadDocument(@Valid @ModelAttribute UploadDocumentRequest request) {
        DocumentResponse response = documentService.uploadDocument(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/patients/{patientId}/documents")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<List<DocumentResponse>> getPatientDocuments(@PathVariable UUID patientId) {
        return ResponseEntity.ok(documentService.getPatientDocuments(patientId));
    }

    @GetMapping("/documents/{documentId}/download")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID documentId, HttpServletRequest request) {
        PatientDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        Resource resource = fileStorageService.loadFileAsResource(document.getFilePath());

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // Default to octet-stream
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"")
                .body(resource);
    }
}
