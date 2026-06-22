package com.hospital.app.treatment.dto.response;

import com.hospital.app.common.enums.FollowUpStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record FollowUpResponse(
        UUID followUpId,
        UUID treatmentCaseId,
        LocalDate followUpDate,
        String reason,
        FollowUpStatus status
) {}
