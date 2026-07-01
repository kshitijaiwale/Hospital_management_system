package com.hospital.app.queue.enums;

public enum QueueStatus {

    /** Patient has checked in and is waiting to be called. */
    WAITING,

    /** Patient is currently with the doctor. */
    IN_CONSULTATION,

    /** Consultation finished normally. */
    COMPLETED,

    /** Patient was called but did not show up. */
    NO_SHOW,

    /** Queue entry was cancelled before consultation. */
    CANCELLED
}
