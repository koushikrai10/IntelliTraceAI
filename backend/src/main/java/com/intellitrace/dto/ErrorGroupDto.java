package com.intellitrace.dto;

/**
 * Data Transfer Object representing a clustered group of similar errors.
 */
public class ErrorGroupDto {

    private Long id;
    private String errorPattern;
    private int occurrenceCount;
    private double percentage;
    private String severity;
    private String exceptionType;
    private String sampleMessage;
    private String firstOccurrence;

    public ErrorGroupDto() {
    }

    public ErrorGroupDto(Long id, String errorPattern, int occurrenceCount, double percentage,
                         String severity, String exceptionType, String sampleMessage, String firstOccurrence) {
        this.id = id;
        this.errorPattern = errorPattern;
        this.occurrenceCount = occurrenceCount;
        this.percentage = percentage;
        this.severity = severity;
        this.exceptionType = exceptionType;
        this.sampleMessage = sampleMessage;
        this.firstOccurrence = firstOccurrence;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getErrorPattern() {
        return errorPattern;
    }

    public void setErrorPattern(String errorPattern) {
        this.errorPattern = errorPattern;
    }

    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(int occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getSampleMessage() {
        return sampleMessage;
    }

    public void setSampleMessage(String sampleMessage) {
        this.sampleMessage = sampleMessage;
    }

    public String getFirstOccurrence() {
        return firstOccurrence;
    }

    public void setFirstOccurrence(String firstOccurrence) {
        this.firstOccurrence = firstOccurrence;
    }
}
