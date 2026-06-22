package com.hospital.app.treatment.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record PrescriptionResponse(
        UUID prescriptionId,
        UUID consultationId,
        String medicationName,
        String dosage,
        String frequency,
        String duration,
        String instructions,
        LocalDateTime createdAt
) {}
