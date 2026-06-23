package com.hospital.app.document.dto.request;

import com.hospital.app.common.enums.DocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadDocumentRequest {
    
    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    private UUID treatmentCaseId;
    
    private UUID consultationId;

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotNull(message = "File is required")
    private MultipartFile file;
}
