package com.intellitrace.parser;

/**
 * POJO representing an individual frame within an execution stack trace
 * (e.g., at com.example.service.UserService.findUser(UserService.java:42)).
 */
public class StackFrame {

    private String className;
    private String methodName;
    private String fileName;
    private int lineNumber;

    public StackFrame() {
    }

    public StackFrame(String className, String methodName, String fileName, int lineNumber) {
        this.className = className;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    // Getters and Setters

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public String toString() {
        return "at " + className + "." + methodName + "(" + (fileName != null ? fileName : "Unknown Source")
                + (lineNumber > 0 ? ":" + lineNumber : "") + ")";
    }
}
