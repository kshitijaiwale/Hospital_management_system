package com.hospital.app.document.dto.response;

import com.hospital.app.common.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private UUID documentId;
    private UUID patientId;
    private UUID treatmentCaseId;
    private UUID consultationId;
    private DocumentType documentType;
    private String fileName;
    private String downloadUrl;
    private UUID uploadedBy;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
}
