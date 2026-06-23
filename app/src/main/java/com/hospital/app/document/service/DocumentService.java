package com.hospital.app.document.service;

import com.hospital.app.document.dto.request.UploadDocumentRequest;
import com.hospital.app.document.dto.response.DocumentResponse;

import java.util.List;
import java.util.UUID;

public interface DocumentService {
    DocumentResponse uploadDocument(UploadDocumentRequest request);
    List<DocumentResponse> getPatientDocuments(UUID patientId);
    DocumentResponse getDocumentById(UUID documentId);
}
