package com.intellitrace.service;

import com.intellitrace.exception.FileValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileValidationService Unit Tests")
class FileValidationServiceTest {

    private FileValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new FileValidationService();
        validationService.setMaxFileSizeBytes(10 * 1024 * 1024); // 10 MB
    }

    @Test
    @DisplayName("Should accept valid log and text files")
    void testValidFile() {
        MockMultipartFile logFile = new MockMultipartFile(
                "file", "server.log", "text/plain", "2024-01-15 INFO test".getBytes(StandardCharsets.UTF_8)
        );
        assertDoesNotThrow(() -> validationService.validate(logFile));

        MockMultipartFile txtFile = new MockMultipartFile(
                "file", "debug.txt", "text/plain", "Some text log".getBytes(StandardCharsets.UTF_8)
        );
        assertDoesNotThrow(() -> validationService.validate(txtFile));
    }

    @Test
    @DisplayName("Should reject empty or null file")
    void testEmptyFileThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.log", "text/plain", new byte[0]
        );
        assertThrows(FileValidationException.class, () -> validationService.validate(emptyFile));
        assertThrows(FileValidationException.class, () -> validationService.validate(null));
    }

    @Test
    @DisplayName("Should reject unsupported file extensions")
    void testInvalidExtensionThrowsException() {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file", "danger.exe", "application/octet-stream", "dummy binary data".getBytes(StandardCharsets.UTF_8)
        );
        assertThrows(FileValidationException.class, () -> validationService.validate(exeFile));

        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "dummy pdf data".getBytes(StandardCharsets.UTF_8)
        );
        assertThrows(FileValidationException.class, () -> validationService.validate(pdfFile));
    }

    @Test
    @DisplayName("Should reject file exceeding size threshold")
    void testOversizedFileThrowsException() {
        validationService.setMaxFileSizeBytes(100); // 100 bytes limit for test

        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "huge.log", "text/plain", new byte[200]
        );
        assertThrows(FileValidationException.class, () -> validationService.validate(largeFile));
    }

    @Test
    @DisplayName("Should reject null or blank filename")
    void testBlankFilenameThrowsException() {
        MockMultipartFile nullNameFile = new MockMultipartFile(
                "file", "", "text/plain", "data".getBytes(StandardCharsets.UTF_8)
        );
        assertThrows(FileValidationException.class, () -> validationService.validate(nullNameFile));
    }
}
