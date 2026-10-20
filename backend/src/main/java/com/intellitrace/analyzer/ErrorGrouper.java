package com.intellitrace.analyzer;

import com.intellitrace.parser.LogEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Component responsible for normalizing error messages and clustering similar log errors
 * into distinct recurring error patterns.
 */
@Component
public class ErrorGrouper {

    // Regex pattern for UUIDs (e.g., "123e4567-e89b-12d3-a456-426614174000")
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b"
    );

    // Regex pattern for hex addresses and Java object hash codes (e.g., "0x7fff5fbff820" or "@5c647e05")
    private static final Pattern HEX_ADDRESS_PATTERN = Pattern.compile(
            "\\b0x[0-9a-fA-F]+\\b|@[0-9a-fA-F]{4,}\\b"
    );

    // Regex pattern for ISO timestamps or standard datetime within log messages
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(
            "\\b\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?\\b"
    );

    // Regex pattern for IP addresses with optional port
    private static final Pattern IP_PATTERN = Pattern.compile(
            "\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(?::\\d+)?\\b"
    );

    // Regex pattern for URL endpoints
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[^\\s]+"
    );

    // Regex pattern for file system paths (Windows & Unix)
    private static final Pattern FILE_PATH_PATTERN = Pattern.compile(
            "(?:[a-zA-Z]:\\\\|/)(?:[a-zA-Z0-9_.-]+[\\\\/])+[a-zA-Z0-9_.-]+"
    );

    // Regex pattern for line numbers (e.g., "(Service.java:42)" -> "(Service.java:<LINE>)" or "line 42" -> "line <LINE>")
    private static final Pattern LINE_NUMBER_PATTERN = Pattern.compile(
            "(?<=:)\\d+(?=\\))|(?<=\\bline\\s+)\\d+", Pattern.CASE_INSENSITIVE
    );

    // Regex pattern for isolated numeric values (IDs, counters, offsets)
    private static final Pattern NUMBER_PATTERN = Pattern.compile(
            "\\b\\d+\\b"
    );

    // Regex pattern for multiple whitespace characters
    private static final Pattern MULTI_WHITESPACE = Pattern.compile(
            "\\s+"
    );

    /**
     * Normalizes a raw error message by masking dynamic variables such as timestamps,
     * UUIDs, hex addresses, file paths, IP addresses, and specific numerical IDs.
     *
     * @param message raw error message or exception line
     * @return normalized pattern template
     */
    public String normalize(String message) {
        if (message == null || message.isBlank()) {
            return "<EMPTY>";
        }

        String normalized = message;
        normalized = UUID_PATTERN.matcher(normalized).replaceAll("<UUID>");
        normalized = HEX_ADDRESS_PATTERN.matcher(normalized).replaceAll("<HEX>");
        normalized = TIMESTAMP_PATTERN.matcher(normalized).replaceAll("<TIMESTAMP>");
        normalized = IP_PATTERN.matcher(normalized).replaceAll("<IP>");
        normalized = URL_PATTERN.matcher(normalized).replaceAll("<URL>");
        normalized = FILE_PATH_PATTERN.matcher(normalized).replaceAll("<PATH>");
        normalized = LINE_NUMBER_PATTERN.matcher(normalized).replaceAll("<LINE>");
        normalized = NUMBER_PATTERN.matcher(normalized).replaceAll("<NUM>");
        normalized = MULTI_WHITESPACE.matcher(normalized).replaceAll(" ");

        return normalized.trim();
    }

    /**
     * Groups log entries by normalized pattern and exception type.
     *
     * @param entries list of all log entries
     * @return sorted list of ErrorGroup instances ordered by occurrence count descending
     */
    public List<ErrorGroup> groupErrors(List<LogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, ErrorGroup> groups = new LinkedHashMap<>();
        int totalErrorOrFatalCount = 0;

        for (LogEntry entry : entries) {
            String level = entry.getLevel() != null ? entry.getLevel().toUpperCase() : "UNKNOWN";
            boolean isErrorLevel = "ERROR".equals(level) || "FATAL".equals(level) || "WARN".equals(level);
            boolean hasException = entry.getExceptionType() != null || entry.getStackTrace() != null;

            if (isErrorLevel || hasException) {
                totalErrorOrFatalCount++;

                String rawText = entry.getMessage() != null && !entry.getMessage().isBlank()
                        ? entry.getMessage()
                        : (entry.getExceptionMessage() != null ? entry.getExceptionMessage() : entry.getRawLine());

                String pattern = normalize(rawText);
                String exType = entry.getExceptionType();
                String groupKey = (exType != null ? exType : "UnknownException") + "::" + pattern;

                ErrorGroup group = groups.computeIfAbsent(groupKey, k ->
                        new ErrorGroup(pattern, level, exType, entry.getMessage(), entry.getTimestamp())
                );
                group.addEntry(entry);
            }
        }

        // Calculate frequency percentage and sort descending by occurrence count
        List<ErrorGroup> result = new ArrayList<>(groups.values());
        final int divisor = totalErrorOrFatalCount > 0 ? totalErrorOrFatalCount : 1;
        for (ErrorGroup group : result) {
            double pct = ((double) group.getOccurrenceCount() / divisor) * 100.0;
            // Round to 2 decimal places
            group.setPercentage(Math.round(pct * 100.0) / 100.0);
        }

        result.sort((a, b) -> Integer.compare(b.getOccurrenceCount(), a.getOccurrenceCount()));
        return result;
    }
}
