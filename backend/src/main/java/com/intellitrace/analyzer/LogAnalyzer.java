package com.intellitrace.analyzer;

import com.intellitrace.parser.LogEntry;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * High-performance analyzer that aggregates log statistics, counts entries across severities,
 * clusters errors using {@link ErrorGrouper}, and produces an analytical summary.
 */
@Component
public class LogAnalyzer {

    private final ErrorGrouper errorGrouper;

    public LogAnalyzer(ErrorGrouper errorGrouper) {
        this.errorGrouper = errorGrouper;
    }

    /**
     * Performs a full analytical evaluation over a list of parsed log entries.
     *
     * @param originalFilename source name of the ingested log file
     * @param entries          parsed log entries
     * @return populated {@link AnalysisResult}
     */
    public AnalysisResult analyze(String originalFilename, List<LogEntry> entries) {
        AnalysisResult result = new AnalysisResult();
        result.setOriginalFilename(originalFilename);
        result.setAnalysisTimestamp(LocalDateTime.now());
        result.setEntries(entries);

        if (entries == null || entries.isEmpty()) {
            result.setTotalEntries(0);
            result.setSummary("No log entries detected in the provided file.");
            return result;
        }

        result.setTotalEntries(entries.size());

        int errorCount = 0;
        int warnCount = 0;
        int infoCount = 0;
        int debugCount = 0;
        int fatalCount = 0;
        int traceCount = 0;
        int unknownCount = 0;

        for (LogEntry entry : entries) {
            String level = entry.getLevel() != null ? entry.getLevel().toUpperCase() : "UNKNOWN";
            switch (level) {
                case "ERROR" -> errorCount++;
                case "WARN", "WARNING" -> warnCount++;
                case "INFO" -> infoCount++;
                case "DEBUG" -> debugCount++;
                case "FATAL", "CRITICAL" -> fatalCount++;
                case "TRACE" -> traceCount++;
                default -> unknownCount++;
            }
        }

        result.setErrorCount(errorCount);
        result.setWarningCount(warnCount);
        result.setInfoCount(infoCount);
        result.setDebugCount(debugCount);
        result.setFatalCount(fatalCount);
        result.setTraceCount(traceCount);
        result.setUnknownCount(unknownCount);

        // Group errors by recurring patterns
        List<ErrorGroup> groups = errorGrouper.groupErrors(entries);
        result.setErrorGroups(groups);

        // Build summary narrative
        String summary = generateSummary(originalFilename, entries.size(), errorCount,
                warnCount, infoCount, debugCount, fatalCount, groups);
        result.setSummary(summary);

        return result;
    }

    /**
     * Filters log entries matching a specific severity level (case-insensitive).
     *
     * @param entries  all entries
     * @param severity target severity (e.g. "ERROR", "WARN")
     * @return filtered list
     */
    public List<LogEntry> filterBySeverity(List<LogEntry> entries, String severity) {
        if (entries == null || severity == null) {
            return new ArrayList<>();
        }
        String target = severity.trim().toUpperCase();
        return entries.stream()
                .filter(e -> e.getLevel() != null && e.getLevel().equalsIgnoreCase(target))
                .collect(Collectors.toList());
    }

    /**
     * Filters log entries containing a specific exception type (case-insensitive substring or match).
     *
     * @param entries       all entries
     * @param exceptionType exception class name (e.g. "NullPointerException")
     * @return filtered list
     */
    public List<LogEntry> filterByExceptionType(List<LogEntry> entries, String exceptionType) {
        if (entries == null || exceptionType == null) {
            return new ArrayList<>();
        }
        String target = exceptionType.trim().toLowerCase();
        return entries.stream()
                .filter(e -> e.getExceptionType() != null && e.getExceptionType().toLowerCase().contains(target))
                .collect(Collectors.toList());
    }

    /**
     * Returns the top N most frequent error groups.
     *
     * @param result analysis result
     * @param limit  number of top groups to return
     * @return list of top error groups
     */
    public List<ErrorGroup> getTopFrequentErrors(AnalysisResult result, int limit) {
        if (result == null || result.getErrorGroups() == null) {
            return new ArrayList<>();
        }
        return result.getErrorGroups().stream()
                .limit(Math.max(0, limit))
                .collect(Collectors.toList());
    }

    /**
     * Generates an informative human-readable executive summary of the log analysis.
     */
    private String generateSummary(String filename, int total, int error, int warn,
                                   int info, int debug, int fatal, List<ErrorGroup> groups) {
        StringBuilder sb = new StringBuilder();
        sb.append("Log Analysis Report for file '").append(filename != null ? filename : "unknown").append("'.\n");
        sb.append("Analyzed ").append(total).append(" total log entries: ");
        sb.append(error).append(" ERROR, ");
        sb.append(warn).append(" WARN, ");
        sb.append(info).append(" INFO, ");
        sb.append(debug).append(" DEBUG, ");
        sb.append(fatal).append(" FATAL.\n");

        if (groups.isEmpty()) {
            sb.append("Health Assessment: Clean execution. No recurring error patterns identified.");
        } else {
            sb.append("Identified ").append(groups.size()).append(" distinct error pattern(s).\n");
            ErrorGroup topError = groups.get(0);
            sb.append("Primary recurring issue: ")
                    .append(topError.getExceptionType() != null ? topError.getExceptionType() + " - " : "")
                    .append(topError.getErrorPattern())
                    .append(" (").append(topError.getOccurrenceCount()).append(" occurrences, ")
                    .append(String.format("%.1f%%", topError.getPercentage())).append(" of issues).");
        }
        return sb.toString();
    }
}
