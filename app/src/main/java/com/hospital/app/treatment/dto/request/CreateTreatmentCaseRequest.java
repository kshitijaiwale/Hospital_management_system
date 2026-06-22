package com.hospital.app.treatment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateTreatmentCaseRequest(
        @NotNull(message = "Patient ID is required")
        UUID patientId,
        
        @NotBlank(message = "Title is required")
        String title,
        
        String diagnosis,
        String caseType
) {}
