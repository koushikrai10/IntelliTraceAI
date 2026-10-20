package com.intellitrace.analyzer;

import com.intellitrace.parser.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LogAnalyzer Unit Tests")
class LogAnalyzerTest {

    private LogAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        ErrorGrouper grouper = new ErrorGrouper();
        analyzer = new LogAnalyzer(grouper);
    }

    @Test
    @DisplayName("Should aggregate severity counts and generate analytical summary")
    void testAnalyzeSeverityCounts() {
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry("2024-01-15 10:00:00", "INFO", "Started server", "Server", "raw", 1));
        entries.add(new LogEntry("2024-01-15 10:00:01", "INFO", "Ready for requests", "Server", "raw", 2));
        entries.add(new LogEntry("2024-01-15 10:00:02", "WARN", "Memory usage high", "Monitor", "raw", 3));
        entries.add(new LogEntry("2024-01-15 10:00:03", "DEBUG", "Packet trace info", "Network", "raw", 4));
        entries.add(new LogEntry("2024-01-15 10:00:04", "ERROR", "NullPointerException: id null", "UserService", "raw", 5));
        entries.add(new LogEntry("2024-01-15 10:00:05", "FATAL", "Database crashed", "DbManager", "raw", 6));

        AnalysisResult result = analyzer.analyze("app.log", entries);

        assertEquals("app.log", result.getOriginalFilename());
        assertEquals(6, result.getTotalEntries());
        assertEquals(1, result.getErrorCount());
        assertEquals(1, result.getWarningCount());
        assertEquals(2, result.getInfoCount());
        assertEquals(1, result.getDebugCount());
        assertEquals(1, result.getFatalCount());

        assertNotNull(result.getSummary());
        assertTrue(result.getSummary().contains("app.log"));
        assertTrue(result.getSummary().contains("Analyzed 6 total log entries"));
    }

    @Test
    @DisplayName("Should handle empty entries gracefully")
    void testAnalyzeEmptyEntries() {
        AnalysisResult result = analyzer.analyze("empty.log", List.of());
        assertEquals(0, result.getTotalEntries());
        assertTrue(result.getSummary().contains("No log entries detected"));
    }

    @Test
    @DisplayName("Should filter entries by severity")
    void testFilterBySeverity() {
        List<LogEntry> entries = List.of(
                new LogEntry("t1", "ERROR", "err1", "log", "raw", 1),
                new LogEntry("t2", "INFO", "inf1", "log", "raw", 2),
                new LogEntry("t3", "ERROR", "err2", "log", "raw", 3)
        );

        List<LogEntry> errors = analyzer.filterBySeverity(entries, "error");
        assertEquals(2, errors.size());

        List<LogEntry> infos = analyzer.filterBySeverity(entries, "INFO");
        assertEquals(1, infos.size());
    }

    @Test
    @DisplayName("Should filter entries by exception type")
    void testFilterByExceptionType() {
        LogEntry e1 = new LogEntry("t1", "ERROR", "msg1", "log", "raw", 1);
        e1.setExceptionType("java.lang.NullPointerException");

        LogEntry e2 = new LogEntry("t2", "ERROR", "msg2", "log", "raw", 2);
        e2.setExceptionType("java.sql.SQLException");

        List<LogEntry> entries = List.of(e1, e2);

        List<LogEntry> npeEntries = analyzer.filterByExceptionType(entries, "NullPointer");
        assertEquals(1, npeEntries.size());
        assertEquals("java.lang.NullPointerException", npeEntries.get(0).getExceptionType());
    }

    @Test
    @DisplayName("Should limit top frequent errors")
    void testGetTopFrequentErrors() {
        List<LogEntry> entries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            LogEntry e = new LogEntry("t", "ERROR", "Socket timeout", "Client", "raw", i);
            e.setExceptionType("java.net.SocketTimeoutException");
            entries.add(e);
        }
        for (int i = 0; i < 2; i++) {
            LogEntry e = new LogEntry("t", "ERROR", "File not found", "IO", "raw", i + 10);
            e.setExceptionType("java.io.FileNotFoundException");
            entries.add(e);
        }

        AnalysisResult result = analyzer.analyze("network.log", entries);
        List<ErrorGroup> top = analyzer.getTopFrequentErrors(result, 1);

        assertEquals(1, top.size());
        assertEquals("java.net.SocketTimeoutException", top.get(0).getExceptionType());
        assertEquals(5, top.get(0).getOccurrenceCount());
    }
}
