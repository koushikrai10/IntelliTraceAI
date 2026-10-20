package com.intellitrace.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for requesting an AI-powered diagnostic explanation
 * and remediation guidance for a specific log error or stack trace.
 */
public class AiExplanationRequestDto {

    @NotBlank(message = "Error pattern must not be blank")
    private String errorPattern;

    private String sampleMessage;

    private String stackTrace;

    private String context;

    public AiExplanationRequestDto() {
    }

    public AiExplanationRequestDto(String errorPattern, String sampleMessage, String stackTrace, String context) {
        this.errorPattern = errorPattern;
        this.sampleMessage = sampleMessage;
        this.stackTrace = stackTrace;
        this.context = context;
    }

    // Getters and Setters

    public String getErrorPattern() {
        return errorPattern;
    }

    public void setErrorPattern(String errorPattern) {
        this.errorPattern = errorPattern;
    }

    public String getSampleMessage() {
        return sampleMessage;
    }

    public void setSampleMessage(String sampleMessage) {
        this.sampleMessage = sampleMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
