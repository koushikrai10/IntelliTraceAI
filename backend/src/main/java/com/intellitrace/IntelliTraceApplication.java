package com.intellitrace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the IntelliTrace AI backend application.
 *
 * IntelliTrace AI is an intelligent log analysis and troubleshooting platform
 * designed to parse, categorize, analyze, and explain application logs using AI.
 */
@SpringBootApplication
public class IntelliTraceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntelliTraceApplication.class, args);
    }
}
