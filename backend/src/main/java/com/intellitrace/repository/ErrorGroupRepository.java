package com.intellitrace.repository;

import com.intellitrace.entity.ErrorGroupRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for managing {@link ErrorGroupRecord} persistence operations.
 */
@Repository
public interface ErrorGroupRepository extends JpaRepository<ErrorGroupRecord, Long> {

    /**
     * Retrieves all error group records associated with a specific analysis session.
     *
     * @param analysisRecordId primary key of the parent analysis record
     * @return list of associated error groups
     */
    List<ErrorGroupRecord> findByAnalysisRecordId(Long analysisRecordId);

    /**
     * Retrieves all error group records associated with a specific analysis session,
     * ordered by occurrence frequency descending.
     *
     * @param analysisRecordId primary key of the parent analysis record
     * @return list of associated error groups ordered by occurrence count
     */
    List<ErrorGroupRecord> findByAnalysisRecordIdOrderByOccurrenceCountDesc(Long analysisRecordId);
}
