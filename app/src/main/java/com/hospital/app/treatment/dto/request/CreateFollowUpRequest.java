package com.hospital.app.treatment.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record CreateFollowUpRequest(
        @NotNull(message = "Treatment Case ID is required")
        UUID treatmentCaseId,

        @NotNull(message = "Follow-up date is required")
        @Future(message = "Follow-up date must be in the future")
        LocalDate followUpDate,

        String reason
) {}
