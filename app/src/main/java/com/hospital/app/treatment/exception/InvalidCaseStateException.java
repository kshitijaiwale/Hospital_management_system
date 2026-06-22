package com.hospital.app.treatment.exception;

public class InvalidCaseStateException extends RuntimeException {
    public InvalidCaseStateException(String message) {
        super(message);
    }
}
