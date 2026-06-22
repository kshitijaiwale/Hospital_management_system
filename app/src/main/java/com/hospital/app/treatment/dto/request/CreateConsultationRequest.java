package com.hospital.app.treatment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateConsultationRequest(
        @NotNull(message = "Treatment Case ID is required")
        UUID treatmentCaseId,
        
        UUID appointmentId,
        String symptoms,
        String diagnosis,
        String clinicalNotes,
        String recommendations
) {}
