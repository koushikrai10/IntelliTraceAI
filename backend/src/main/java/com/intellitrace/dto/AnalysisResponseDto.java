package com.intellitrace.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing the full response of an analysis run,
 * including metrics, summary, and error breakdown.
 */
public class AnalysisResponseDto {

    private Long id;
    private String originalFilename;
    private LocalDateTime analysisTimestamp;
    private int totalEntries;
    private int errorCount;
    private int warningCount;
    private int infoCount;
    private int debugCount;
    private int fatalCount;
    private String summary;
    private List<ErrorGroupDto> errorGroups = new ArrayList<>();

    public AnalysisResponseDto() {
    }

    public AnalysisResponseDto(Long id, String originalFilename, LocalDateTime analysisTimestamp,
                               int totalEntries, int errorCount, int warningCount, int infoCount,
                               int debugCount, int fatalCount, String summary,
                               List<ErrorGroupDto> errorGroups) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.analysisTimestamp = analysisTimestamp;
        this.totalEntries = totalEntries;
        this.errorCount = errorCount;
        this.warningCount = warningCount;
        this.infoCount = infoCount;
        this.debugCount = debugCount;
        this.fatalCount = fatalCount;
        this.summary = summary;
        if (errorGroups != null) {
            this.errorGroups = errorGroups;
        }
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

    public List<ErrorGroupDto> getErrorGroups() {
        return errorGroups;
    }

    public void setErrorGroups(List<ErrorGroupDto> errorGroups) {
        this.errorGroups = errorGroups;
    }
}
