package com.hospital.app.queue.service;

import com.hospital.app.queue.dto.response.PatientQueueStatusResponse;
import com.hospital.app.queue.dto.response.QueueEntryResponse;

import java.util.List;
import java.util.UUID;

public interface QueueService {

    /** Receptionist checks a patient in — issues a sequential queue token. */
    QueueEntryResponse checkIn(UUID appointmentId);

    /** Moves a WAITING entry to IN_CONSULTATION. */
    QueueEntryResponse startConsultation(UUID queueEntryId);

    /** Moves an IN_CONSULTATION entry to COMPLETED. */
    QueueEntryResponse completeConsultation(UUID queueEntryId);

    /** Marks a WAITING entry as NO_SHOW. */
    QueueEntryResponse markNoShow(UUID queueEntryId);

    /** Cancels a WAITING queue entry. */
    QueueEntryResponse cancelQueueEntry(UUID queueEntryId);

    /** Returns today's full queue ordered by token number. */
    List<QueueEntryResponse> getTodaysQueue();

    /** Returns the entry currently IN_CONSULTATION today. */
    QueueEntryResponse getCurrentlyServing();

    /** Returns the next WAITING entry (lowest token number) for today. */
    QueueEntryResponse getNextInQueue();

    /** Patient-facing: returns their position and the currently-serving number. */
    PatientQueueStatusResponse getMyQueueStatus(UUID appointmentId);
}
