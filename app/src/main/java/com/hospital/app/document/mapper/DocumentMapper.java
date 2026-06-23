package com.hospital.app.document.mapper;

import com.hospital.app.document.dto.response.DocumentResponse;
import com.hospital.app.document.entity.PatientDocument;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentResponse toResponse(PatientDocument document) {
        if (document == null) return null;

        return DocumentResponse.builder()
                .documentId(document.getDocumentId())
                .patientId(document.getPatient() != null ? document.getPatient().getPatientId() : null)
                .treatmentCaseId(document.getTreatmentCase() != null ? document.getTreatmentCase().getTreatmentCaseId() : null)
                .consultationId(document.getConsultation() != null ? document.getConsultation().getConsultationId() : null)
                .documentType(document.getDocumentType())
                .fileName(document.getFileName())
                .uploadedBy(document.getUploadedBy() != null ? document.getUploadedBy().getUserId() : null)
                .uploadedByName(document.getUploadedBy() != null ? document.getUploadedBy().getName() : null)
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}
