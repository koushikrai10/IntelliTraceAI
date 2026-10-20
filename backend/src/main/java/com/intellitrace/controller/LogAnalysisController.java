package com.intellitrace.controller;

import com.intellitrace.dto.AnalysisResponseDto;
import com.intellitrace.service.LogAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * REST Controller providing endpoints for initiating log file analyses
 * and fetching existing analysis reports.
 */
@RestController
@RequestMapping("/api/analysis")
public class LogAnalysisController {

    private final LogAnalysisService logAnalysisService;

    public LogAnalysisController(LogAnalysisService logAnalysisService) {
        this.logAnalysisService = logAnalysisService;
    }

    /**
     * Ingests and analyzes a log file uploaded via multipart form-data.
     *
     * @param file uploaded log file (.log, .txt, etc.)
     * @return analysis summary and clustered error groups
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisResponseDto> uploadLogFile(@RequestParam("file") MultipartFile file) {
        AnalysisResponseDto response = logAnalysisService.analyzeLogFile(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Ingests and analyzes raw log text provided in request body.
     *
     * @param payload map containing 'filename' and 'content'
     * @return analysis summary and clustered error groups
     */
    @PostMapping("/raw")
    public ResponseEntity<AnalysisResponseDto> analyzeRawLog(@RequestBody Map<String, String> payload) {
        String filename = payload.getOrDefault("filename", "raw_log_input.log");
        String content = payload.getOrDefault("content", "");
        AnalysisResponseDto response = logAnalysisService.analyzeLogContent(filename, content);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves an existing log analysis report by its ID.
     *
     * @param id analysis ID
     * @return analysis response DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<AnalysisResponseDto> getAnalysisById(@PathVariable("id") Long id) {
        AnalysisResponseDto response = logAnalysisService.getAnalysisById(id);
        return ResponseEntity.ok(response);
    }
}
