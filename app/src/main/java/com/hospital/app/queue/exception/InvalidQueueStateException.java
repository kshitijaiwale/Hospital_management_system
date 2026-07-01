package com.hospital.app.queue.exception;

public class InvalidQueueStateException extends RuntimeException {
    public InvalidQueueStateException(String message) {
        super(message);
    }
}