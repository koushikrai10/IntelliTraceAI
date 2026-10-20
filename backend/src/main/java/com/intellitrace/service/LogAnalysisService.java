package com.intellitrace.service;

import com.intellitrace.analyzer.AnalysisResult;
import com.intellitrace.analyzer.ErrorGroup;
import com.intellitrace.analyzer.LogAnalyzer;
import com.intellitrace.dto.AnalysisResponseDto;
import com.intellitrace.dto.ErrorGroupDto;
import com.intellitrace.entity.AnalysisRecord;
import com.intellitrace.entity.ErrorGroupRecord;
import com.intellitrace.exception.AnalysisNotFoundException;
import com.intellitrace.parser.LogEntry;
import com.intellitrace.parser.LogParser;
import com.intellitrace.repository.AnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Core orchestration service for the log analysis workflow:
 * 1. File Validation
 * 2. Log Parsing & Stack Trace Extraction
 * 3. Log Analysis & Error Clustering
 * 4. JPA Entity Persistence
 * 5. DTO Transformation
 */
@Service
public class LogAnalysisService {

    private final FileValidationService fileValidationService;
    private final LogParser logParser;
    private final LogAnalyzer logAnalyzer;
    private final AnalysisRepository analysisRepository;

    public LogAnalysisService(FileValidationService fileValidationService,
                              LogParser logParser,
                              LogAnalyzer logAnalyzer,
                              AnalysisRepository analysisRepository) {
        this.fileValidationService = fileValidationService;
        this.logParser = logParser;
        this.logAnalyzer = logAnalyzer;
        this.analysisRepository = analysisRepository;
    }

    /**
     * Executes end-to-end analysis on an uploaded multipart log file.
     *
     * @param file uploaded log file
     * @return analysis response DTO with aggregated statistics and error clusters
     */
    @Transactional
    public AnalysisResponseDto analyzeLogFile(MultipartFile file) {
        fileValidationService.validate(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded_log.log";
        List<LogEntry> entries;
        try (InputStream inputStream = file.getInputStream()) {
            entries = logParser.parse(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded log stream: " + e.getMessage(), e);
        }

        AnalysisResult result = logAnalyzer.analyze(originalFilename, entries);
        AnalysisRecord entity = persistAnalysis(result);
        return mapToDto(entity);
    }

    /**
     * Executes end-to-end analysis on raw log text content.
     *
     * @param filename logical filename
     * @param content  raw log text
     * @return analysis response DTO
     */
    @Transactional
    public AnalysisResponseDto analyzeLogContent(String filename, String content) {
        fileValidationService.validate(filename, content != null ? content.length() : 0, content == null || content.isBlank());

        List<LogEntry> entries = logParser.parse(content);
        AnalysisResult result = logAnalyzer.analyze(filename, entries);
        AnalysisRecord entity = persistAnalysis(result);
        return mapToDto(entity);
    }

    /**
     * Retrieves an existing analysis result by its unique database identifier.
     *
     * @param id primary key of analysis record
     * @return analysis response DTO
     * @throws AnalysisNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public AnalysisResponseDto getAnalysisById(Long id) {
        AnalysisRecord record = analysisRepository.findById(id)
                .orElseThrow(() -> new AnalysisNotFoundException(id));
        return mapToDto(record);
    }

    /**
     * Saves the analytical results into relational database tables.
     */
    private AnalysisRecord persistAnalysis(AnalysisResult result) {
        AnalysisRecord record = new AnalysisRecord(
                result.getOriginalFilename(),
                result.getAnalysisTimestamp(),
                result.getTotalEntries(),
                result.getErrorCount(),
                result.getWarningCount(),
                result.getInfoCount(),
                result.getDebugCount(),
                result.getFatalCount(),
                result.getSummary()
        );

        if (result.getErrorGroups() != null) {
            for (ErrorGroup group : result.getErrorGroups()) {
                ErrorGroupRecord groupRecord = new ErrorGroupRecord(
                        group.getErrorPattern(),
                        group.getOccurrenceCount(),
                        group.getSeverity(),
                        group.getExceptionType(),
                        group.getSampleMessage(),
                        group.getFirstOccurrence()
                );
                record.addErrorGroup(groupRecord);
            }
        }

        return analysisRepository.save(record);
    }

    /**
     * Maps a JPA AnalysisRecord entity to its corresponding client DTO.
     */
    public AnalysisResponseDto mapToDto(AnalysisRecord record) {
        List<ErrorGroupDto> groupDtos = new ArrayList<>();
        int totalIssues = record.getErrorCount() + record.getFatalCount() + record.getWarningCount();
        final int divisor = totalIssues > 0 ? totalIssues : 1;

        if (record.getErrorGroups() != null) {
            for (ErrorGroupRecord gr : record.getErrorGroups()) {
                double pct = ((double) gr.getOccurrenceCount() / divisor) * 100.0;
                double roundedPct = Math.round(pct * 100.0) / 100.0;

                ErrorGroupDto dto = new ErrorGroupDto(
                        gr.getId(),
                        gr.getErrorPattern(),
                        gr.getOccurrenceCount(),
                        roundedPct,
                        gr.getSeverity(),
                        gr.getExceptionType(),
                        gr.getSampleMessage(),
                        gr.getFirstOccurrence()
                );
                groupDtos.add(dto);
            }
        }

        return new AnalysisResponseDto(
                record.getId(),
                record.getOriginalFilename(),
                record.getAnalysisTimestamp(),
                record.getTotalEntries(),
                record.getErrorCount(),
                record.getWarningCount(),
                record.getInfoCount(),
                record.getDebugCount(),
                record.getFatalCount(),
                record.getSummary(),
                groupDtos
        );
    }
}
