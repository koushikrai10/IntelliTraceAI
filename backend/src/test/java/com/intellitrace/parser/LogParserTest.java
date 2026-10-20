package com.intellitrace.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LogParser Unit Tests")
class LogParserTest {

    private LogParser logParser;

    @BeforeEach
    void setUp() {
        StackTraceParser stackTraceParser = new StackTraceParser();
        logParser = new LogParser(stackTraceParser);
    }

    @Test
    @DisplayName("Should parse standard Spring Boot logs")
    void testParseStandardSpringBootLog() {
        String logContent = """
                2024-01-15T10:30:45.123+00:00 INFO 12345 --- [main] c.e.service.UserService : Application initialized successfully
                2024-01-15T10:30:46.456+00:00 WARN 12345 --- [main] c.e.service.CacheService : Cache miss for key 45
                """;

        List<LogEntry> entries = logParser.parse(logContent);
        assertEquals(2, entries.size());

        LogEntry entry1 = entries.get(0);
        assertEquals("INFO", entry1.getLevel());
        assertEquals("c.e.service.UserService", entry1.getLoggerName());
        assertEquals("Application initialized successfully", entry1.getMessage());
        assertEquals(1, entry1.getLineNumber());

        LogEntry entry2 = entries.get(1);
        assertEquals("WARN", entry2.getLevel());
        assertEquals("c.e.service.CacheService", entry2.getLoggerName());
        assertEquals("Cache miss for key 45", entry2.getMessage());
    }

    @Test
    @DisplayName("Should parse standard Log4j / Logback formatted logs")
    void testParseLog4jFormat() {
        String logContent = """
                2024-01-15 10:30:45.123 ERROR [com.example.Service] - NullPointerException: user object is null
                2024-01-15 10:30:46.789 DEBUG [com.example.Service] - Debugging payload dump
                """;

        List<LogEntry> entries = logParser.parse(logContent);
        assertEquals(2, entries.size());

        LogEntry errorEntry = entries.get(0);
        assertEquals("ERROR", errorEntry.getLevel());
        assertEquals("com.example.Service", errorEntry.getLoggerName());
        assertEquals("NullPointerException: user object is null", errorEntry.getMessage());

        LogEntry debugEntry = entries.get(1);
        assertEquals("DEBUG", debugEntry.getLevel());
        assertEquals("Debugging payload dump", debugEntry.getMessage());
    }

    @Test
    @DisplayName("Should parse bracketed log formats")
    void testParseBracketedLogFormat() {
        String logContent = """
                [2024-01-15 10:30:45.123] [WARN] [com.example.Security] Access token expired for user 99
                [2024-01-15 10:30:46.000] [INFO] [com.example.Security] Token refreshed
                """;

        List<LogEntry> entries = logParser.parse(logContent);
        assertEquals(2, entries.size());
        assertEquals("WARN", entries.get(0).getLevel());
        assertEquals("INFO", entries.get(1).getLevel());
    }

    @Test
    @DisplayName("Should associate multiline stack traces with preceding error entry")
    void testParseMultilineStackTrace() {
        String logContent = """
                2024-01-15 10:30:45.123 ERROR [com.example.OrderService] - Failed to process order 1001
                java.lang.NullPointerException: Object reference is null
                \tat com.example.OrderService.calculateTotal(OrderService.java:55)
                \tat com.example.OrderService.process(OrderService.java:30)
                Caused by: java.lang.IllegalStateException: Subtotal cannot be zero
                \tat com.example.TaxCalculator.calc(TaxCalculator.java:12)
                \t... 15 more
                2024-01-15 10:30:46.000 INFO [com.example.OrderService] - Next transaction started
                """;

        List<LogEntry> entries = logParser.parse(logContent);
        assertEquals(2, entries.size());

        LogEntry errorEntry = entries.get(0);
        assertEquals("ERROR", errorEntry.getLevel());
        assertNotNull(errorEntry.getStackTrace());
        assertTrue(errorEntry.getStackTrace().contains("calculateTotal"));
        assertTrue(errorEntry.getStackTrace().contains("TaxCalculator"));
        assertEquals("java.lang.NullPointerException", errorEntry.getExceptionType());
        assertFalse(errorEntry.getStackFrames().isEmpty());

        LogEntry nextEntry = entries.get(1);
        assertEquals("INFO", nextEntry.getLevel());
        assertNull(nextEntry.getStackTrace());
    }

    @Test
    @DisplayName("Should handle orphan leading lines gracefully as UNKNOWN entries")
    void testParseOrphanLeadingLines() {
        String logContent = """
                SYSTEM BOOTSTRAP INITIALIZING...
                BUILD VERSION 1.0.0
                2024-01-15 10:30:45.123 INFO Starting Spring Boot Context
                """;

        List<LogEntry> entries = logParser.parse(logContent);
        assertEquals(3, entries.size());
        assertEquals("UNKNOWN", entries.get(0).getLevel());
        assertEquals("UNKNOWN", entries.get(1).getLevel());
        assertEquals("INFO", entries.get(2).getLevel());
    }

    @Test
    @DisplayName("Should normalize various severity level representations")
    void testSeverityNormalization() {
        assertEquals("WARN", LogParser.normalizeLevel("WARNING"));
        assertEquals("ERROR", LogParser.normalizeLevel("SEVERE"));
        assertEquals("FATAL", LogParser.normalizeLevel("CRITICAL"));
        assertEquals("INFO", LogParser.normalizeLevel("INFO"));
        assertEquals("DEBUG", LogParser.normalizeLevel("DEBUG"));
        assertEquals("TRACE", LogParser.normalizeLevel("TRACE"));
    }

    @Test
    @DisplayName("Should parse from InputStream as well as String")
    void testParseInputStream() throws IOException {
        String log = "2024-01-15 10:30:45.123 INFO Service active\n";
        ByteArrayInputStream is = new ByteArrayInputStream(log.getBytes(StandardCharsets.UTF_8));
        List<LogEntry> entries = logParser.parse(is);
        assertEquals(1, entries.size());
        assertEquals("INFO", entries.get(0).getLevel());
    }

    @Test
    @DisplayName("Should return empty list on null or blank content")
    void testEmptyOrNullInput() {
        assertTrue(logParser.parse((String) null).isEmpty());
        assertTrue(logParser.parse("   ").isEmpty());
    }
}
