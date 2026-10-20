package com.intellitrace.exception;

/**
 * Exception thrown when an uploaded log file violates validation rules
 * such as empty content, forbidden file extensions, or exceeding size thresholds.
 */
public class FileValidationException extends RuntimeException {

    public FileValidationException(String message) {
        super(message);
    }

    public FileValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
