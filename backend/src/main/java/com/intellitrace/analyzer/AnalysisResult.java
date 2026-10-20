package com.intellitrace.analyzer;

import com.intellitrace.parser.LogEntry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * POJO holding comprehensive analytical results produced by {@link LogAnalyzer}.
 * Contains metrics, grouped error patterns, raw entries, and executive summary text.
 */
public class AnalysisResult {

    private String originalFilename;
    private LocalDateTime analysisTimestamp;
    private int totalEntries;
    private int errorCount;
    private int warningCount;
    private int infoCount;
    private int debugCount;
    private int fatalCount;
    private int traceCount;
    private int unknownCount;
    private String summary;
    private List<ErrorGroup> errorGroups = new ArrayList<>();
    private List<LogEntry> entries = new ArrayList<>();

    public AnalysisResult() {
        this.analysisTimestamp = LocalDateTime.now();
    }

    // Getters and Setters

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

    public int getTraceCount() {
        return traceCount;
    }

    public void setTraceCount(int traceCount) {
        this.traceCount = traceCount;
    }

    public int getUnknownCount() {
        return unknownCount;
    }

    public void setUnknownCount(int unknownCount) {
        this.unknownCount = unknownCount;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<ErrorGroup> getErrorGroups() {
        return errorGroups;
    }

    public void setErrorGroups(List<ErrorGroup> errorGroups) {
        this.errorGroups = errorGroups != null ? errorGroups : new ArrayList<>();
    }

    public List<LogEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<LogEntry> entries) {
        this.entries = entries != null ? entries : new ArrayList<>();
    }
}
