package com.intellitrace.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA entity representing a normalized, clustered group of log errors.
 * Belongs to an {@link AnalysisRecord}.
 */
@Entity
@Table(name = "error_group_records")
public class ErrorGroupRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_record_id", nullable = false)
    private AnalysisRecord analysisRecord;

    @Column(name = "error_pattern", columnDefinition = "TEXT", nullable = false)
    private String errorPattern;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount;

    @Column(name = "severity", nullable = false, length = 50)
    private String severity;

    @Column(name = "exception_type", length = 255)
    private String exceptionType;

    @Column(name = "sample_message", columnDefinition = "TEXT")
    private String sampleMessage;

    @Column(name = "first_occurrence", length = 100)
    private String firstOccurrence;

    public ErrorGroupRecord() {
    }

    public ErrorGroupRecord(String errorPattern, int occurrenceCount, String severity,
                            String exceptionType, String sampleMessage, String firstOccurrence) {
        this.errorPattern = errorPattern;
        this.occurrenceCount = occurrenceCount;
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

    public AnalysisRecord getAnalysisRecord() {
        return analysisRecord;
    }

    public void setAnalysisRecord(AnalysisRecord analysisRecord) {
        this.analysisRecord = analysisRecord;
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
