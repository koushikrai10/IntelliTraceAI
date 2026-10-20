package com.intellitrace.analyzer;

import com.intellitrace.parser.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorGrouper Unit Tests")
class ErrorGrouperTest {

    private ErrorGrouper grouper;

    @BeforeEach
    void setUp() {
        grouper = new ErrorGrouper();
    }

    @Test
    @DisplayName("Should normalize UUIDs in messages")
    void testNormalizeUuids() {
        String msg = "User session e4eaaaf2-d142-11e1-b3e4-080027620cdd expired";
        String normalized = grouper.normalize(msg);
        assertEquals("User session <UUID> expired", normalized);
    }

    @Test
    @DisplayName("Should normalize hex addresses and object hashes")
    void testNormalizeHex() {
        String msg1 = "Fault at memory address 0x7fff5fbff820 during execution";
        assertEquals("Fault at memory address <HEX> during execution", grouper.normalize(msg1));

        String msg2 = "Unexpected instance of com.example.Session@4d7e1886";
        assertEquals("Unexpected instance of com.example.Session<HEX>", grouper.normalize(msg2));
    }

    @Test
    @DisplayName("Should normalize timestamps, IP addresses, and URLs")
    void testNormalizeTimestampsIpsUrls() {
        String msg = "Request to https://api.internal.com/v1 at 2024-01-15 10:30:45 from 192.168.1.100 failed";
        String normalized = grouper.normalize(msg);
        assertTrue(normalized.contains("<URL>"));
        assertTrue(normalized.contains("<TIMESTAMP>"));
        assertTrue(normalized.contains("<IP>"));
    }

    @Test
    @DisplayName("Should normalize numbers and line numbers")
    void testNormalizeNumbers() {
        String msg = "Record with ID 94827 failed at line 145 with error code 500";
        String normalized = grouper.normalize(msg);
        assertEquals("Record with ID <NUM> failed at line <LINE> with error code <NUM>", normalized);
    }

    @Test
    @DisplayName("Should cluster recurring errors into a single ErrorGroup with dynamic parameters normalized")
    void testGroupErrorsClustering() {
        LogEntry e1 = new LogEntry("2024-01-15 10:00:00", "ERROR", "NullPointerException: user 101 not found", "UserService", "raw1", 1);
        e1.setExceptionType("java.lang.NullPointerException");

        LogEntry e2 = new LogEntry("2024-01-15 10:01:00", "ERROR", "NullPointerException: user 202 not found", "UserService", "raw2", 5);
        e2.setExceptionType("java.lang.NullPointerException");

        LogEntry e3 = new LogEntry("2024-01-15 10:02:00", "ERROR", "NullPointerException: user 303 not found", "UserService", "raw3", 9);
        e3.setExceptionType("java.lang.NullPointerException");

        LogEntry e4 = new LogEntry("2024-01-15 10:05:00", "ERROR", "Connection refused to database host", "DbService", "raw4", 15);
        e4.setExceptionType("java.sql.SQLException");

        List<ErrorGroup> groups = grouper.groupErrors(List.of(e1, e2, e3, e4));

        assertEquals(2, groups.size());

        // First group should be the NPE group with 3 occurrences (75%)
        ErrorGroup topGroup = groups.get(0);
        assertEquals("java.lang.NullPointerException", topGroup.getExceptionType());
        assertEquals("NullPointerException: user <NUM> not found", topGroup.getErrorPattern());
        assertEquals(3, topGroup.getOccurrenceCount());
        assertEquals(75.0, topGroup.getPercentage());
        assertEquals("2024-01-15 10:00:00", topGroup.getFirstOccurrence());

        // Second group should be the SQL group with 1 occurrence (25%)
        ErrorGroup secondGroup = groups.get(1);
        assertEquals("java.sql.SQLException", secondGroup.getExceptionType());
        assertEquals(1, secondGroup.getOccurrenceCount());
        assertEquals(25.0, secondGroup.getPercentage());
    }
}
