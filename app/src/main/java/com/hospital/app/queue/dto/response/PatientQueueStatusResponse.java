package com.hospital.app.queue.dto.response;

import com.hospital.app.queue.enums.QueueStatus;
import lombok.Builder;

import java.time.LocalDate;

/**
 * Lightweight patient-facing response — shows the patient their position
 * in today's queue without exposing other patients' details.
 */
@Builder
public record PatientQueueStatusResponse(
        int myQueueNumber,
        QueueStatus myStatus,
        /** How many WAITING patients are ahead. Null when not in WAITING status. */
        Integer patientsAhead,
        /** The queue number currently being served. Null if nobody is in consultation. */
        Integer currentlyServingNumber,
        LocalDate queueDate
) {}
