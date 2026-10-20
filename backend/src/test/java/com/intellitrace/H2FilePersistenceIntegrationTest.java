package com.intellitrace;

import com.intellitrace.entity.AnalysisRecord;
import com.intellitrace.entity.ErrorGroupRecord;
import com.intellitrace.repository.AnalysisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2-file")
@DisplayName("H2 File-Based Persistent Profile Integration Test")
class H2FilePersistenceIntegrationTest {

    @Autowired
    private AnalysisRepository analysisRepository;

    @Test
    @Transactional
    @DisplayName("Should successfully persist and retrieve analysis records and error groups in h2-file profile")
    void testPersistAndRetrieveInH2FileProfile() {
        AnalysisRecord record = new AnalysisRecord(
                "persistent-test.log",
                LocalDateTime.now(),
                100,
                5,
                2,
                80,
                10,
                3,
                "Test summary for persistent file-based H2 profile"
        );

        ErrorGroupRecord group = new ErrorGroupRecord(
                "NullPointerException at line <LINE>",
                5,
                "ERROR",
                "java.lang.NullPointerException",
                "NullPointerException: sample message",
                "2024-01-15 10:00:00"
        );
        record.addErrorGroup(group);

        AnalysisRecord saved = analysisRepository.save(record);
        assertNotNull(saved.getId(), "Persisted record should have an auto-generated primary key");

        AnalysisRecord retrieved = analysisRepository.findById(saved.getId()).orElse(null);
        assertNotNull(retrieved, "Should retrieve persisted record from file-based H2 database");
        assertEquals("persistent-test.log", retrieved.getOriginalFilename());
        assertEquals(5, retrieved.getErrorCount());
        assertEquals(1, retrieved.getErrorGroups().size());
        assertEquals("java.lang.NullPointerException", retrieved.getErrorGroups().get(0).getExceptionType());
    }
}
