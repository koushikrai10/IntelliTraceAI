package com.intellitrace.service;

import com.intellitrace.dto.AnalysisResponseDto;
import com.intellitrace.entity.AnalysisRecord;
import com.intellitrace.exception.AnalysisNotFoundException;
import com.intellitrace.repository.AnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing history records and auditing of prior log analysis operations.
 */
@Service
public class AnalysisHistoryService {

    private final AnalysisRepository analysisRepository;
    private final LogAnalysisService logAnalysisService;

    public AnalysisHistoryService(AnalysisRepository analysisRepository,
                                  LogAnalysisService logAnalysisService) {
        this.analysisRepository = analysisRepository;
        this.logAnalysisService = logAnalysisService;
    }

    /**
     * Retrieves all historic log analysis reports ordered from newest to oldest.
     *
     * @return list of analysis response DTOs
     */
    @Transactional(readOnly = true)
    public List<AnalysisResponseDto> getAllHistory() {
        return analysisRepository.findAllByOrderByAnalysisTimestampDesc().stream()
                .map(logAnalysisService::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single historic analysis report by identifier.
     *
     * @param id primary key of the record
     * @return analysis response DTO
     * @throws AnalysisNotFoundException if record not found
     */
    @Transactional(readOnly = true)
    public AnalysisResponseDto getHistoryById(Long id) {
        AnalysisRecord record = analysisRepository.findById(id)
                .orElseThrow(() -> new AnalysisNotFoundException(id));
        return logAnalysisService.mapToDto(record);
    }

    /**
     * Deletes a historic analysis report along with its nested error group records.
     *
     * @param id primary key of the record to delete
     * @throws AnalysisNotFoundException if record does not exist
     */
    @Transactional
    public void deleteHistory(Long id) {
        if (!analysisRepository.existsById(id)) {
            throw new AnalysisNotFoundException(id);
        }
        analysisRepository.deleteById(id);
    }

    /**
     * Searches history records filtered by original file name.
     *
     * @param query search query
     * @return list of matching analysis records
     */
    @Transactional(readOnly = true)
    public List<AnalysisResponseDto> searchHistoryByFilename(String query) {
        return analysisRepository.findByOriginalFilenameContainingIgnoreCase(query).stream()
                .map(logAnalysisService::mapToDto)
                .collect(Collectors.toList());
    }
}
