package com.hospital.app.treatment.dto.response;

import com.hospital.app.common.enums.CaseStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TreatmentCaseResponse(
        UUID treatmentCaseId,
        UUID patientId,
        String patientName,
        String title,
        String diagnosis,
        String caseType,
        CaseStatus status,
        LocalDateTime openDate,
        LocalDateTime closeDate
) {}
