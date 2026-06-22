package com.hospital.app.treatment.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ConsultationResponse(
        UUID consultationId,
        UUID treatmentCaseId,
        UUID appointmentId,
        UUID doctorId,
        String doctorName,
        LocalDateTime consultationDate,
        String symptoms,
        String diagnosis,
        String clinicalNotes,
        String recommendations
) {}
