package com.intellitrace.repository;

import com.intellitrace.entity.AnalysisRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for managing {@link AnalysisRecord} persistence operations.
 */
@Repository
public interface AnalysisRepository extends JpaRepository<AnalysisRecord, Long> {

    /**
     * Retrieves all log analysis records ordered by execution timestamp in descending order.
     *
     * @return list of analysis records newest first
     */
    List<AnalysisRecord> findAllByOrderByAnalysisTimestampDesc();

    /**
     * Searches analysis records matching a specific original filename (case-insensitive).
     *
     * @param originalFilename filename substring
     * @return list of matching analysis records
     */
    List<AnalysisRecord> findByOriginalFilenameContainingIgnoreCase(String originalFilename);
}
