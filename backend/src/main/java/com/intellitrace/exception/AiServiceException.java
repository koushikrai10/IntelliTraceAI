package com.intellitrace.exception;

/**
 * Exception thrown when communication with an external AI service provider
 * fails, times out, or encounters authentication issues.
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
