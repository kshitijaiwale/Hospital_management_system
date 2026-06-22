package com.hospital.app.treatment.dto.request;

import com.hospital.app.common.enums.FollowUpStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UpdateFollowUpRequest(
        @NotNull(message = "Status is required")
        FollowUpStatus status
) {}
