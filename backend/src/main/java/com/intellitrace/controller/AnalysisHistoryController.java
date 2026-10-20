package com.intellitrace.controller;

import com.intellitrace.dto.AnalysisResponseDto;
import com.intellitrace.service.AnalysisHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for managing historical log analysis records, including
 * listing past reports, searching by filename, and deleting history records.
 */
@RestController
@RequestMapping("/api/history")
public class AnalysisHistoryController {

    private final AnalysisHistoryService historyService;

    public AnalysisHistoryController(AnalysisHistoryService historyService) {
        this.historyService = historyService;
    }

    /**
     * Retrieves all analysis history records, optionally filtered by filename query.
     *
     * @param filename optional search query
     * @return list of analysis response DTOs
     */
    @GetMapping
    public ResponseEntity<List<AnalysisResponseDto>> getAllHistory(
            @RequestParam(value = "filename", required = false) String filename) {
        if (filename != null && !filename.isBlank()) {
            return ResponseEntity.ok(historyService.searchHistoryByFilename(filename));
        }
        return ResponseEntity.ok(historyService.getAllHistory());
    }

    /**
     * Retrieves a single historic analysis report.
     *
     * @param id analysis ID
     * @return analysis response DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<AnalysisResponseDto> getHistoryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(historyService.getHistoryById(id));
    }

    /**
     * Deletes a historic analysis report and all associated error groups.
     *
     * @param id analysis ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHistory(@PathVariable("id") Long id) {
        historyService.deleteHistory(id);
        return ResponseEntity.noContent().build();
    }
}
