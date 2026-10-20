package com.intellitrace.parser;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core log parser capable of ingesting diverse log formats, detecting multiline
 * stack traces, standardizing log severity levels, and retaining raw line data.
 */
@Component
public class LogParser {

    private final StackTraceParser stackTraceParser;

    // Pattern 1: Spring Boot / Modern Logback (e.g., "2024-01-15T10:30:45.123+01:00 ERROR 12345 --- [main] c.e.Service : message")
    private static final Pattern PATTERN_SPRING_BOOT = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{3,6})?(?:Z|[+-]\\d{2}:?\\d{2})?)\\s+"
                    + "(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|SEVERE|CRITICAL)\\s+"
                    + "(?:\\d+\\s+---\\s+)?(?:\\[([^\\]]*)\\]\\s+)?([a-zA-Z0-9_.$]+)\\s*:\\s*(.*)$"
    );

    // Pattern 2: Standard Log4j/Logback (e.g., "2024-01-15 10:30:45.123 ERROR [com.example.Service] - message" or "2024-01-15 10:30:45.123 [main] ERROR com.example.Service - message")
    private static final Pattern PATTERN_LOG4J = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{3,6})?)\\s+"
                    + "(?:\\[([^\\]]*)\\]\\s+)?(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|SEVERE|CRITICAL)\\s+"
                    + "\\[?([a-zA-Z0-9_.$]+)?\\]?\\s*(?:-\\s*)?:?\\s*(.*)$"
    );

    // Pattern 3: Bracketed log format (e.g., "[2024-01-15 10:30:45.123] [ERROR] [com.example.Service] message")
    private static final Pattern PATTERN_BRACKETED = Pattern.compile(
            "^\\[(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{3,6})?)\\]\\s+"
                    + "\\[(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|SEVERE|CRITICAL)\\]\\s+"
                    + "(?:\\[([^\\]]+)\\]\\s*)?(?:-\\s*)?(.*)$"
    );

    // Pattern 4: Fallback timestamp + level + message (e.g., "2024-01-15 10:30:45 ERROR Something bad happened")
    private static final Pattern PATTERN_FALLBACK = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{3,6})?)\\s+"
                    + "(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|SEVERE|CRITICAL)\\s+(.*)$"
    );

    public LogParser(StackTraceParser stackTraceParser) {
        this.stackTraceParser = stackTraceParser;
    }

    /**
     * Parses the complete log content from a String into a list of {@link LogEntry} objects.
     *
     * @param logContent string containing the raw log file content
     * @return list of structured LogEntry objects
     */
    public List<LogEntry> parse(String logContent) {
        if (logContent == null || logContent.isBlank()) {
            return new ArrayList<>();
        }
        try (BufferedReader reader = new BufferedReader(new StringReader(logContent))) {
            return parse(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read log string content", e);
        }
    }

    /**
     * Parses the log content from an InputStream into a list of {@link LogEntry} objects.
     *
     * @param inputStream stream of log file bytes
     * @return list of structured LogEntry objects
     * @throws IOException if reading the stream fails
     */
    public List<LogEntry> parse(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return new ArrayList<>();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        return parse(reader);
    }

    /**
     * Internal line-by-line streaming parser that builds entries and aggregates stack traces.
     */
    private List<LogEntry> parse(BufferedReader reader) throws IOException {
        List<LogEntry> entries = new ArrayList<>();
        LogEntry currentEntry = null;
        StringBuilder currentStackTrace = new StringBuilder();
        String line;
        int lineNumber = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            ParsedHeader header = tryParseHeader(line, lineNumber);

            if (header != null) {
                // Finalize the previous entry if present
                if (currentEntry != null) {
                    finalizeEntry(currentEntry, currentStackTrace);
                    entries.add(currentEntry);
                }

                currentEntry = new LogEntry(
                        header.timestamp,
                        header.level,
                        header.message,
                        header.logger,
                        line,
                        lineNumber
                );
                currentStackTrace.setLength(0);

                // Check if the initial message already contains an exception line
                if (header.message != null && header.message.matches(".*[a-zA-Z0-9_.$]*(?:Exception|Error|Throwable):?.*")) {
                    currentStackTrace.append(header.message).append(System.lineSeparator());
                }
            } else {
                // Continuation line or stack trace line
                if (currentEntry != null) {
                    currentStackTrace.append(line).append(System.lineSeparator());
                    currentEntry.setRawLine(currentEntry.getRawLine() + "\n" + line);
                } else {
                    // Line encountered before any recognized log header
                    LogEntry orphanEntry = new LogEntry(
                            null,
                            "UNKNOWN",
                            line.trim(),
                            "UNKNOWN",
                            line,
                            lineNumber
                    );
                    entries.add(orphanEntry);
                }
            }
        }

        // Finalize the last entry
        if (currentEntry != null) {
            finalizeEntry(currentEntry, currentStackTrace);
            entries.add(currentEntry);
        }

        return entries;
    }

    /**
     * Enriches the log entry with parsed stack trace details and root cause metadata.
     */
    private void finalizeEntry(LogEntry entry, StringBuilder stackTraceBuilder) {
        String trace = stackTraceBuilder.toString().trim();
        if (!trace.isEmpty()) {
            entry.setStackTrace(trace);
            entry.setStackFrames(stackTraceParser.parseStackFrames(trace));
            String exType = stackTraceParser.extractExceptionType(trace);
            if (exType != null) {
                entry.setExceptionType(exType);
            }
            String exMsg = stackTraceParser.extractExceptionMessage(trace);
            if (exMsg != null) {
                entry.setExceptionMessage(exMsg);
            }
        } else if (entry.getMessage() != null) {
            // Check if exception type is in the message itself
            String exType = stackTraceParser.extractExceptionType(entry.getMessage());
            if (exType != null) {
                entry.setExceptionType(exType);
                entry.setExceptionMessage(stackTraceParser.extractExceptionMessage(entry.getMessage()));
            }
        }
    }

    /**
     * Tries matching known regex patterns against the candidate line.
     */
    private ParsedHeader tryParseHeader(String line, int lineNumber) {
        if (line == null || line.isBlank()) {
            return null;
        }

        // Pattern 1: Spring Boot
        Matcher m1 = PATTERN_SPRING_BOOT.matcher(line);
        if (m1.find()) {
            String timestamp = m1.group(1);
            String level = normalizeLevel(m1.group(2));
            String logger = m1.group(4);
            String message = m1.group(5) != null ? m1.group(5).trim() : "";
            return new ParsedHeader(timestamp, level, logger, message);
        }

        // Pattern 2: Log4j / Logback
        Matcher m2 = PATTERN_LOG4J.matcher(line);
        if (m2.find()) {
            String timestamp = m2.group(1);
            String level = normalizeLevel(m2.group(3));
            String logger = m2.group(4) != null ? m2.group(4) : (m2.group(2) != null ? m2.group(2) : "ROOT");
            String message = m2.group(5) != null ? m2.group(5).trim() : "";
            return new ParsedHeader(timestamp, level, logger, message);
        }

        // Pattern 3: Bracketed
        Matcher m3 = PATTERN_BRACKETED.matcher(line);
        if (m3.find()) {
            String timestamp = m3.group(1);
            String level = normalizeLevel(m3.group(2));
            String logger = m3.group(3) != null ? m3.group(3) : "ROOT";
            String message = m3.group(4) != null ? m3.group(4).trim() : "";
            return new ParsedHeader(timestamp, level, logger, message);
        }

        // Pattern 4: Fallback
        Matcher m4 = PATTERN_FALLBACK.matcher(line);
        if (m4.find()) {
            String timestamp = m4.group(1);
            String level = normalizeLevel(m4.group(2));
            String message = m4.group(3) != null ? m4.group(3).trim() : "";
            return new ParsedHeader(timestamp, level, "ROOT", message);
        }

        return null;
    }

    /**
     * Normalizes severity keywords to standard names: INFO, WARN, ERROR, DEBUG, FATAL, TRACE.
     */
    public static String normalizeLevel(String rawLevel) {
        if (rawLevel == null) {
            return "UNKNOWN";
        }
        String upper = rawLevel.trim().toUpperCase();
        return switch (upper) {
            case "WARNING" -> "WARN";
            case "SEVERE" -> "ERROR";
            case "CRITICAL" -> "FATAL";
            case "INFO", "WARN", "ERROR", "DEBUG", "FATAL", "TRACE" -> upper;
            default -> upper;
        };
    }

    /**
     * Internal container holding parsed header tokens.
     */
    private static class ParsedHeader {
        final String timestamp;
        final String level;
        final String logger;
        final String message;

        ParsedHeader(String timestamp, String level, String logger, String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.logger = logger;
            this.message = message;
        }
    }
}
