package com.intellitrace.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * POJO representing a single parsed log entry, including any associated
 * multiline stack traces and extracted exception details.
 */
public class LogEntry {

    private String timestamp;
    private String level;
    private String message;
    private String loggerName;
    private String rawLine;
    private int lineNumber;
    private String stackTrace;
    private String exceptionType;
    private String exceptionMessage;
    private List<StackFrame> stackFrames = new ArrayList<>();

    public LogEntry() {
    }

    public LogEntry(String timestamp, String level, String message, String loggerName,
                    String rawLine, int lineNumber) {
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
        this.loggerName = loggerName;
        this.rawLine = rawLine;
        this.lineNumber = lineNumber;
    }

    // Getters and Setters

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getRawLine() {
        return rawLine;
    }

    public void setRawLine(String rawLine) {
        this.rawLine = rawLine;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public List<StackFrame> getStackFrames() {
        return stackFrames;
    }

    public void setStackFrames(List<StackFrame> stackFrames) {
        this.stackFrames = stackFrames != null ? stackFrames : new ArrayList<>();
    }
}
