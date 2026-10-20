package com.intellitrace.controller;

import com.intellitrace.service.ReportExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for generating and streaming downloadable analysis export reports
 * in CSV and TXT formats.
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportExportService reportExportService;

    public ReportController(ReportExportService reportExportService) {
        this.reportExportService = reportExportService;
    }

    /**
     * Generates and downloads a CSV error report for a given analysis ID.
     *
     * @param id analysis ID
     * @return CSV file stream
     */
    @GetMapping("/{id}/csv")
    public ResponseEntity<byte[]> downloadCsvReport(@PathVariable("id") Long id) {
        byte[] csvData = reportExportService.generateCsvReport(id);
        String filename = "analysis-report-" + id + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    /**
     * Generates and downloads a plain text diagnostic report for a given analysis ID.
     *
     * @param id analysis ID
     * @return TXT file stream
     */
    @GetMapping("/{id}/txt")
    public ResponseEntity<byte[]> downloadTxtReport(@PathVariable("id") Long id) {
        byte[] txtData = reportExportService.generateTxtReport(id);
        String filename = "analysis-report-" + id + ".txt";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/plain; charset=UTF-8"))
                .body(txtData);
    }
}
