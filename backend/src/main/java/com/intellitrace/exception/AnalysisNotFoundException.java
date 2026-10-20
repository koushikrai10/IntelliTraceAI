package com.intellitrace.exception;

/**
 * Exception thrown when a requested log analysis record cannot be found in the database.
 */
public class AnalysisNotFoundException extends RuntimeException {

    public AnalysisNotFoundException(Long id) {
        super("Analysis record not found with ID: " + id);
    }

    public AnalysisNotFoundException(String message) {
        super(message);
    }
}
