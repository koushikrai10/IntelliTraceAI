package com.intellitrace.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility parser for analyzing stack traces.
 * Extracts exception classes, messages, individual stack frames, and nested cause chains.
 */
@Component
public class StackTraceParser {

    // Regex for standard stack frame: "at com.example.Class.method(Class.java:42)" or "(Native Method)"
    private static final Pattern STACK_FRAME_PATTERN = Pattern.compile(
            "^\\s*at\\s+([a-zA-Z0-9_.$]+)\\.([a-zA-Z0-9_$<]+)\\((?:([^:)]+)(?::(\\d+))?|Native Method|Unknown Source)\\)"
    );

    // Regex for "Caused by: com.example.CustomException: message"
    private static final Pattern CAUSED_BY_PATTERN = Pattern.compile(
            "^\\s*Caused by:\\s+([a-zA-Z0-9_.$]+)(?::\\s*(.*))?$"
    );

    // Regex for top-level exception header: "com.example.Exception: something went wrong"
    private static final Pattern EXCEPTION_HEADER_PATTERN = Pattern.compile(
            "([a-zA-Z0-9_.$]*(?:Exception|Error|Throwable|Failure|Fault))(?::\\s*(.*))?"
    );

    /**
     * Checks whether a given raw line looks like part of a Java stack trace.
     *
     * @param line the line of text
     * @return true if the line matches stack trace characteristics
     */
    public boolean isStackTraceLine(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.trim();
        return trimmed.startsWith("at ")
                || trimmed.startsWith("Caused by:")
                || trimmed.startsWith("Suppressed:")
                || (trimmed.startsWith("...") && trimmed.endsWith("more"))
                || trimmed.matches("^[a-zA-Z0-9_.$]+(?:Exception|Error|Throwable|Failure):?.*$");
    }

    /**
     * Extracts the primary exception class name from a stack trace or log message.
     *
     * @param text the stack trace text or log message
     * @return fully-qualified exception name, or null if none identified
     */
    public String extractExceptionType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = EXCEPTION_HEADER_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extracts the exception message accompanying the exception class name.
     *
     * @param text the stack trace text or log message
     * @return the error message or empty string if absent
     */
    public String extractExceptionMessage(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = EXCEPTION_HEADER_PATTERN.matcher(text);
        if (matcher.find() && matcher.group(2) != null) {
            return matcher.group(2).trim();
        }
        return null;
    }

    /**
     * Parses all stack frames from a multiline stack trace into structured {@link StackFrame} objects.
     *
     * @param stackTrace multiline stack trace text
     * @return list of parsed StackFrame items
     */
    public List<StackFrame> parseStackFrames(String stackTrace) {
        List<StackFrame> frames = new ArrayList<>();
        if (stackTrace == null || stackTrace.isBlank()) {
            return frames;
        }

        String[] lines = stackTrace.split("\\R");
        for (String line : lines) {
            Matcher matcher = STACK_FRAME_PATTERN.matcher(line);
            if (matcher.find()) {
                String className = matcher.group(1);
                String methodName = matcher.group(2);
                String fileName = matcher.group(3);
                int lineNumber = -1;
                if (matcher.group(4) != null) {
                    try {
                        lineNumber = Integer.parseInt(matcher.group(4));
                    } catch (NumberFormatException ignored) {
                        lineNumber = -1;
                    }
                }
                frames.add(new StackFrame(className, methodName, fileName, lineNumber));
            }
        }
        return frames;
    }

    /**
     * Extracts all nested root-cause exception lines from the stack trace (e.g. "Caused by: ...").
     *
     * @param stackTrace multiline stack trace text
     * @return list of nested cause strings
     */
    public List<String> extractNestedCauses(String stackTrace) {
        List<String> causes = new ArrayList<>();
        if (stackTrace == null || stackTrace.isBlank()) {
            return causes;
        }

        String[] lines = stackTrace.split("\\R");
        for (String line : lines) {
            Matcher matcher = CAUSED_BY_PATTERN.matcher(line);
            if (matcher.find()) {
                String causeType = matcher.group(1);
                String causeMsg = matcher.group(2) != null ? matcher.group(2) : "";
                causes.add(causeType + (!causeMsg.isBlank() ? ": " + causeMsg : ""));
            }
        }
        return causes;
    }

    /**
     * Returns the deepest root cause found in the stack trace, or the top exception if no nested cause.
     *
     * @param stackTrace multiline stack trace text
     * @return root cause exception description
     */
    public String extractRootCause(String stackTrace) {
        List<String> nested = extractNestedCauses(stackTrace);
        if (!nested.isEmpty()) {
            return nested.get(nested.size() - 1);
        }
        String type = extractExceptionType(stackTrace);
        String msg = extractExceptionMessage(stackTrace);
        if (type != null) {
            return type + (msg != null && !msg.isBlank() ? ": " + msg : "");
        }
        return null;
    }
}
