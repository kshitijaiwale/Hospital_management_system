package com.hospital.app.queue.dto.response;

import com.hospital.app.queue.enums.QueueStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Full queue entry detail — used in receptionist-facing responses.
 */
@Builder
public record QueueEntryResponse(
        UUID queueEntryId,
        UUID appointmentId,
        UUID patientId,
        String patientName,
        String patientNumber,
        LocalDate queueDate,
        int queueNumber,
        QueueStatus status,
        /** Dynamic: how many WAITING entries have a lower queueNumber today. Null when not WAITING. */
        Integer position,
        LocalDateTime checkInTime,
        LocalDateTime consultationStartTime,
        LocalDateTime consultationEndTime,
        LocalDateTime createdAt
) {}