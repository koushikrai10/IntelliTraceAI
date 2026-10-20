package com.intellitrace.controller;

import com.intellitrace.dto.AnalysisResponseDto;
import com.intellitrace.dto.ErrorGroupDto;
import com.intellitrace.exception.AnalysisNotFoundException;
import com.intellitrace.exception.GlobalExceptionHandler;
import com.intellitrace.service.LogAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogAnalysisController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("LogAnalysisController WebMvc Tests")
class LogAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogAnalysisService logAnalysisService;

    @Test
    @DisplayName("POST /api/analysis/upload should return 201 Created and analysis DTO")
    void testUploadLogFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "app.log", "text/plain", "2024-01-15 INFO test".getBytes(StandardCharsets.UTF_8)
        );

        AnalysisResponseDto responseDto = new AnalysisResponseDto(
                1L,
                "app.log",
                LocalDateTime.now(),
                50,
                2,
                1,
                47,
                0,
                0,
                "Analysis complete",
                List.of(new ErrorGroupDto(10L, "Pattern", 2, 100.0, "ERROR", "NullPointerException", "Sample", "t"))
        );

        when(logAnalysisService.analyzeLogFile(any())).thenReturn(responseDto);

        mockMvc.perform(multipart("/api/analysis/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.originalFilename").value("app.log"))
                .andExpect(jsonPath("$.totalEntries").value(50))
                .andExpect(jsonPath("$.errorCount").value(2))
                .andExpect(jsonPath("$.errorGroups[0].errorPattern").value("Pattern"));
    }

    @Test
    @DisplayName("GET /api/analysis/{id} should return 200 OK when found")
    void testGetAnalysisByIdSuccess() throws Exception {
        AnalysisResponseDto responseDto = new AnalysisResponseDto(
                5L, "server.log", LocalDateTime.now(), 20, 0, 0, 20, 0, 0, "Clean log", List.of()
        );

        when(logAnalysisService.getAnalysisById(5L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/analysis/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.originalFilename").value("server.log"))
                .andExpect(jsonPath("$.totalEntries").value(20));
    }

    @Test
    @DisplayName("GET /api/analysis/{id} should return 404 Not Found when not present")
    void testGetAnalysisByIdNotFound() throws Exception {
        when(logAnalysisService.getAnalysisById(999L)).thenThrow(new AnalysisNotFoundException(999L));

        mockMvc.perform(get("/api/analysis/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Analysis record not found with ID: 999"));
    }
}
