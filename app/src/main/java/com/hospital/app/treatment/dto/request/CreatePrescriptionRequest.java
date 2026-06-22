package com.hospital.app.treatment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreatePrescriptionRequest(
        @NotBlank(message = "Medication name is required")
        String medicationName,
        
        String dosage,
        String frequency,
        String duration,
        String instructions
) {}
