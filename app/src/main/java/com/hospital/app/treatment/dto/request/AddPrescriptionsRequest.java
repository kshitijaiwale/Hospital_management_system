package com.hospital.app.treatment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record AddPrescriptionsRequest(
        @NotNull(message = "Consultation ID is required")
        UUID consultationId,

        @NotEmpty(message = "At least one prescription is required")
        @Valid
        List<CreatePrescriptionRequest> prescriptions
) {}
