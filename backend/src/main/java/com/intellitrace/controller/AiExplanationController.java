package com.intellitrace.controller;

import com.intellitrace.ai.AiService;
import com.intellitrace.dto.AiExplanationRequestDto;
import com.intellitrace.dto.AiExplanationResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for generating AI-powered diagnostic explanations,
 * root-cause deductions, and guided troubleshooting steps.
 */
@RestController
@RequestMapping("/api/ai")
public class AiExplanationController {

    private final AiService aiService;

    public AiExplanationController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * Generates an AI-driven explanation for a specific error pattern or stack trace.
     *
     * @param request diagnostic payload containing error pattern, stack trace, and context
     * @return explanation, root cause, and remediation steps
     */
    @PostMapping("/explain")
    public ResponseEntity<AiExplanationResponseDto> explainError(
            @Valid @RequestBody AiExplanationRequestDto request) {
        AiExplanationResponseDto explanation = aiService.explainError(request);
        return ResponseEntity.ok(explanation);
    }
}
