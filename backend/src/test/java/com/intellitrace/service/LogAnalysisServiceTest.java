package com.intellitrace.service;

import com.intellitrace.analyzer.AnalysisResult;
import com.intellitrace.analyzer.ErrorGroup;
import com.intellitrace.analyzer.LogAnalyzer;
import com.intellitrace.dto.AnalysisResponseDto;
import com.intellitrace.entity.AnalysisRecord;
import com.intellitrace.entity.ErrorGroupRecord;
import com.intellitrace.exception.AnalysisNotFoundException;
import com.intellitrace.parser.LogEntry;
import com.intellitrace.parser.LogParser;
import com.intellitrace.repository.AnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogAnalysisService Unit Tests")
class LogAnalysisServiceTest {

    @Mock
    private FileValidationService fileValidationService;

    @Mock
    private LogParser logParser;

    @Mock
    private LogAnalyzer logAnalyzer;

    @Mock
    private AnalysisRepository analysisRepository;

    @InjectMocks
    private LogAnalysisService analysisService;

    private AnalysisRecord sampleRecord;

    @BeforeEach
    void setUp() {
        sampleRecord = new AnalysisRecord(
                "test.log",
                LocalDateTime.now(),
                100,
                5,
                2,
                90,
                3,
                0,
                "Sample summary text"
        );
        sampleRecord.setId(1L);

        ErrorGroupRecord groupRecord = new ErrorGroupRecord(
                "NullPointerException in Service",
                5,
                "ERROR",
                "java.lang.NullPointerException",
                "NullPointerException: id is null",
                "2024-01-15 10:00:00"
        );
        groupRecord.setId(10L);
        sampleRecord.addErrorGroup(groupRecord);
    }

    @Test
    @DisplayName("Should orchestrate full analysis lifecycle successfully")
    void testAnalyzeLogFileSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.log", "text/plain", "log data".getBytes(StandardCharsets.UTF_8)
        );

        List<LogEntry> entries = List.of(new LogEntry("t", "ERROR", "msg", "log", "raw", 1));
        when(logParser.parse(any(InputStream.class))).thenReturn(entries);

        AnalysisResult result = new AnalysisResult();
        result.setOriginalFilename("test.log");
        result.setTotalEntries(1);
        result.setErrorCount(1);
        result.setSummary("Summary test");
        ErrorGroup eg = new ErrorGroup("Pattern", "ERROR", "ExType", "Sample", "t");
        eg.setOccurrenceCount(1);
        result.setErrorGroups(List.of(eg));

        when(logAnalyzer.analyze(eq("test.log"), eq(entries))).thenReturn(result);
        when(analysisRepository.save(any(AnalysisRecord.class))).thenReturn(sampleRecord);

        AnalysisResponseDto dto = analysisService.analyzeLogFile(file);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("test.log", dto.getOriginalFilename());
        assertEquals(100, dto.getTotalEntries());
        assertEquals(5, dto.getErrorCount());

        verify(fileValidationService).validate(file);
        verify(logParser).parse(any(InputStream.class));
        verify(logAnalyzer).analyze(eq("test.log"), eq(entries));
        verify(analysisRepository).save(any(AnalysisRecord.class));
    }

    @Test
    @DisplayName("Should retrieve analysis record by ID")
    void testGetAnalysisByIdFound() {
        when(analysisRepository.findById(1L)).thenReturn(Optional.of(sampleRecord));

        AnalysisResponseDto dto = analysisService.getAnalysisById(1L);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("test.log", dto.getOriginalFilename());
        assertEquals(1, dto.getErrorGroups().size());
        assertEquals("java.lang.NullPointerException", dto.getErrorGroups().get(0).getExceptionType());
    }

    @Test
    @DisplayName("Should throw AnalysisNotFoundException when ID does not exist")
    void testGetAnalysisByIdNotFound() {
        when(analysisRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AnalysisNotFoundException.class, () -> analysisService.getAnalysisById(999L));
    }
}
