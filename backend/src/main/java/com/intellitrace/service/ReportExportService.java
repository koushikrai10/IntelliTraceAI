package com.intellitrace.service;

import com.intellitrace.entity.AnalysisRecord;
import com.intellitrace.entity.ErrorGroupRecord;
import com.intellitrace.exception.AnalysisNotFoundException;
import com.intellitrace.repository.AnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Service for compiling and exporting analysis findings into standard downloadable
 * report formats including comma-separated values (CSV) and plain text (TXT).
 */
@Service
public class ReportExportService {

    private final AnalysisRepository analysisRepository;

    public ReportExportService(AnalysisRepository analysisRepository) {
        this.analysisRepository = analysisRepository;
    }

    /**
     * Generates a CSV report summarizing error groups and frequency data.
     *
     * @param analysisId id of the target analysis record
     * @return raw UTF-8 CSV bytes
     */
    @Transactional(readOnly = true)
    public byte[] generateCsvReport(Long analysisId) {
        AnalysisRecord record = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));

        StringBuilder csv = new StringBuilder();
        // CSV Header
        csv.append("ID,Severity,Exception Type,Occurrences,First Occurrence,Error Pattern,Sample Message\n");

        if (record.getErrorGroups() != null) {
            for (ErrorGroupRecord gr : record.getErrorGroups()) {
                csv.append(gr.getId()).append(",");
                csv.append(escapeCsv(gr.getSeverity())).append(",");
                csv.append(escapeCsv(gr.getExceptionType())).append(",");
                csv.append(gr.getOccurrenceCount()).append(",");
                csv.append(escapeCsv(gr.getFirstOccurrence())).append(",");
                csv.append(escapeCsv(gr.getErrorPattern())).append(",");
                csv.append(escapeCsv(gr.getSampleMessage())).append("\n");
            }
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generates a formatted plain-text diagnostic summary report.
     *
     * @param analysisId id of the target analysis record
     * @return raw UTF-8 text bytes
     */
    @Transactional(readOnly = true)
    public byte[] generateTxtReport(Long analysisId) {
        AnalysisRecord record = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder txt = new StringBuilder();
        txt.append("================================================================================\n");
        txt.append("                   INTELLITRACE AI - LOG ANALYSIS REPORT                      \n");
        txt.append("================================================================================\n\n");
        txt.append("File Analyzed     : ").append(record.getOriginalFilename()).append("\n");
        txt.append("Timestamp         : ").append(record.getAnalysisTimestamp().format(dtf)).append("\n");
        txt.append("Total Log Entries : ").append(record.getTotalEntries()).append("\n");
        txt.append("  - ERROR         : ").append(record.getErrorCount()).append("\n");
        txt.append("  - FATAL         : ").append(record.getFatalCount()).append("\n");
        txt.append("  - WARN          : ").append(record.getWarningCount()).append("\n");
        txt.append("  - INFO          : ").append(record.getInfoCount()).append("\n");
        txt.append("  - DEBUG         : ").append(record.getDebugCount()).append("\n\n");

        txt.append("--------------------------------------------------------------------------------\n");
        txt.append("EXECUTIVE SUMMARY\n");
        txt.append("--------------------------------------------------------------------------------\n");
        txt.append(record.getSummary() != null ? record.getSummary() : "No summary available.").append("\n\n");

        txt.append("--------------------------------------------------------------------------------\n");
        txt.append("IDENTIFIED ERROR PATTERNS (").append(record.getErrorGroups() != null ? record.getErrorGroups().size() : 0).append(" Total)\n");
        txt.append("--------------------------------------------------------------------------------\n");

        if (record.getErrorGroups() == null || record.getErrorGroups().isEmpty()) {
            txt.append("No errors or exceptions were recorded in this log session.\n");
        } else {
            int rank = 1;
            for (ErrorGroupRecord gr : record.getErrorGroups()) {
                txt.append(String.format("[%d] Pattern: %s\n", rank++, gr.getErrorPattern()));
                txt.append("    Severity        : ").append(gr.getSeverity()).append("\n");
                txt.append("    Exception Type  : ").append(gr.getExceptionType() != null ? gr.getExceptionType() : "N/A").append("\n");
                txt.append("    Occurrences     : ").append(gr.getOccurrenceCount()).append("\n");
                txt.append("    First Seen      : ").append(gr.getFirstOccurrence() != null ? gr.getFirstOccurrence() : "N/A").append("\n");
                txt.append("    Sample Message  : ").append(gr.getSampleMessage() != null ? gr.getSampleMessage() : "N/A").append("\n\n");
            }
        }

        txt.append("================================================================================\n");
        txt.append("                           END OF INTELLITRACE REPORT                           \n");
        txt.append("================================================================================\n");

        return txt.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "\"\"";
        }
        String clean = value.replace("\"", "\"\"");
        return "\"" + clean + "\"";
    }
}
