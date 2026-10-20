package com.intellitrace.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a completed log analysis session.
 * Stores high-level metrics, summary information, and links to categorized error groups.
 */
@Entity
@Table(name = "analysis_records")
public class AnalysisRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "analysis_timestamp", nullable = false)
    private LocalDateTime analysisTimestamp;

    @Column(name = "total_entries", nullable = false)
    private int totalEntries;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "warning_count", nullable = false)
    private int warningCount;

    @Column(name = "info_count", nullable = false)
    private int infoCount;

    @Column(name = "debug_count", nullable = false)
    private int debugCount;

    @Column(name = "fatal_count", nullable = false)
    private int fatalCount;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @OneToMany(mappedBy = "analysisRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ErrorGroupRecord> errorGroups = new ArrayList<>();

    public AnalysisRecord() {
    }

    public AnalysisRecord(String originalFilename, LocalDateTime analysisTimestamp, int totalEntries,
                          int errorCount, int warningCount, int infoCount, int debugCount,
                          int fatalCount, String summary) {
        this.originalFilename = originalFilename;
        this.analysisTimestamp = analysisTimestamp;
        this.totalEntries = totalEntries;
        this.errorCount = errorCount;
        this.warningCount = warningCount;
        this.infoCount = infoCount;
        this.debugCount = debugCount;
        this.fatalCount = fatalCount;
        this.summary = summary;
    }

    public void addErrorGroup(ErrorGroupRecord errorGroup) {
        errorGroups.add(errorGroup);
        errorGroup.setAnalysisRecord(this);
    }

    public void removeErrorGroup(ErrorGroupRecord errorGroup) {
        errorGroups.remove(errorGroup);
        errorGroup.setAnalysisRecord(null);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public LocalDateTime getAnalysisTimestamp() {
        return analysisTimestamp;
    }

    public void setAnalysisTimestamp(LocalDateTime analysisTimestamp) {
        this.analysisTimestamp = analysisTimestamp;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int totalEntries) {
        this.totalEntries = totalEntries;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public int getInfoCount() {
        return infoCount;
    }

    public void setInfoCount(int infoCount) {
        this.infoCount = infoCount;
    }

    public int getDebugCount() {
        return debugCount;
    }

    public void setDebugCount(int debugCount) {
        this.debugCount = debugCount;
    }

    public int getFatalCount() {
        return fatalCount;
    }

    public void setFatalCount(int fatalCount) {
        this.fatalCount = fatalCount;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<ErrorGroupRecord> getErrorGroups() {
        return errorGroups;
    }

    public void setErrorGroups(List<ErrorGroupRecord> errorGroups) {
        this.errorGroups = errorGroups;
    }
}
