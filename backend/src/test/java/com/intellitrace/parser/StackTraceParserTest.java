package com.intellitrace.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StackTraceParser Unit Tests")
class StackTraceParserTest {

    private StackTraceParser parser;

    @BeforeEach
    void setUp() {
        parser = new StackTraceParser();
    }

    @Test
    @DisplayName("Should accurately detect stack trace lines")
    void testIsStackTraceLine() {
        assertTrue(parser.isStackTraceLine("\tat com.example.service.UserService.findUser(UserService.java:42)"));
        assertTrue(parser.isStackTraceLine("at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1089)"));
        assertTrue(parser.isStackTraceLine("Caused by: java.lang.IllegalStateException: Database connection failed"));
        assertTrue(parser.isStackTraceLine("\t... 25 more"));
        assertTrue(parser.isStackTraceLine("java.lang.NullPointerException: Object is null"));

        assertFalse(parser.isStackTraceLine("2024-01-15 10:30:45.123 INFO Starting service"));
        assertFalse(parser.isStackTraceLine("User successfully logged in with ID: 12345"));
        assertFalse(parser.isStackTraceLine(null));
        assertFalse(parser.isStackTraceLine(""));
    }

    @Test
    @DisplayName("Should extract exception type from stack trace header or message")
    void testExtractExceptionType() {
        String trace = "java.lang.NullPointerException: Cannot invoke method on null object\n\tat com.test.App.run(App.java:10)";
        assertEquals("java.lang.NullPointerException", parser.extractExceptionType(trace));

        String customEx = "com.intellitrace.exception.FileValidationException: File size exceeds threshold";
        assertEquals("com.intellitrace.exception.FileValidationException", parser.extractExceptionType(customEx));

        assertNull(parser.extractExceptionType("Normal log entry with no exceptions"));
        assertNull(parser.extractExceptionType(null));
    }

    @Test
    @DisplayName("Should extract exception message from header")
    void testExtractExceptionMessage() {
        String trace = "java.lang.IllegalArgumentException: Invalid user ID provided: -1\n\tat com.test.App.run(App.java:10)";
        assertEquals("Invalid user ID provided: -1", parser.extractExceptionMessage(trace));

        String noMsg = "java.lang.NullPointerException";
        assertNull(parser.extractExceptionMessage(noMsg));
    }

    @Test
    @DisplayName("Should parse individual stack frames with line numbers")
    void testParseStackFrames() {
        String trace = """
                java.lang.RuntimeException: Test
                \tat com.example.service.OrderService.placeOrder(OrderService.java:99)
                \tat com.example.controller.OrderController.submit(OrderController.java:45)
                \tat org.springframework.cglib.proxy.MethodProxy.invoke(Native Method)
                """;

        List<StackFrame> frames = parser.parseStackFrames(trace);
        assertEquals(3, frames.size());

        StackFrame frame1 = frames.get(0);
        assertEquals("com.example.service.OrderService", frame1.getClassName());
        assertEquals("placeOrder", frame1.getMethodName());
        assertEquals("OrderService.java", frame1.getFileName());
        assertEquals(99, frame1.getLineNumber());

        StackFrame frame2 = frames.get(1);
        assertEquals("com.example.controller.OrderController", frame2.getClassName());
        assertEquals("submit", frame2.getMethodName());
        assertEquals("OrderController.java", frame2.getFileName());
        assertEquals(45, frame2.getLineNumber());

        StackFrame frame3 = frames.get(2);
        assertEquals("org.springframework.cglib.proxy.MethodProxy", frame3.getClassName());
        assertEquals("invoke", frame3.getMethodName());
        assertEquals(-1, frame3.getLineNumber()); // Native Method
    }

    @Test
    @DisplayName("Should extract nested causes and root cause")
    void testExtractNestedCauses() {
        String trace = """
                org.springframework.web.util.NestedServletException: Request processing failed
                \tat org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1014)
                Caused by: java.lang.IllegalStateException: Cache lookup failed
                \tat com.example.cache.RedisCache.get(RedisCache.java:55)
                Caused by: java.net.ConnectException: Connection refused: no further information
                \tat java.base/sun.nio.ch.Net.pollConnect(Native Method)
                \t... 30 more
                """;

        List<String> causes = parser.extractNestedCauses(trace);
        assertEquals(2, causes.size());
        assertEquals("java.lang.IllegalStateException: Cache lookup failed", causes.get(0));
        assertEquals("java.net.ConnectException: Connection refused: no further information", causes.get(1));

        String rootCause = parser.extractRootCause(trace);
        assertEquals("java.net.ConnectException: Connection refused: no further information", rootCause);
    }
}
