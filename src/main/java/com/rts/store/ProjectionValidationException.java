package com.rts.store;

public class ProjectionValidationException extends RuntimeException {
    public ProjectionValidationException(String message) {
        super(message);
    }

    public ProjectionValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
