package com.intellitrace.analyzer;

import com.intellitrace.parser.LogEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * POJO representing a group of recurring similar errors clustered
 * by their normalized pattern and exception type.
 */
public class ErrorGroup {

    private String errorPattern;
    private int occurrenceCount;
    private double percentage;
    private String severity;
    private String exceptionType;
    private String sampleMessage;
    private String firstOccurrence;
    private List<LogEntry> entries = new ArrayList<>();

    public ErrorGroup() {
    }

    public ErrorGroup(String errorPattern, String severity, String exceptionType, String sampleMessage, String firstOccurrence) {
        this.errorPattern = errorPattern;
        this.severity = severity;
        this.exceptionType = exceptionType;
        this.sampleMessage = sampleMessage;
        this.firstOccurrence = firstOccurrence;
        this.occurrenceCount = 0;
        this.percentage = 0.0;
    }

    public void addEntry(LogEntry entry) {
        entries.add(entry);
        occurrenceCount++;
        if (firstOccurrence == null && entry.getTimestamp() != null) {
            firstOccurrence = entry.getTimestamp();
        }
        if (sampleMessage == null && entry.getMessage() != null) {
            sampleMessage = entry.getMessage();
        }
    }

    // Getters and Setters

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

    public List<LogEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<LogEntry> entries) {
        this.entries = entries != null ? entries : new ArrayList<>();
        this.occurrenceCount = this.entries.size();
    }
}
