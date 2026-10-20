package com.intellitrace.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO returned by the AI explanation endpoint, providing
 * diagnostic insights, root-cause deduction, and actionable remediation steps.
 */
public class AiExplanationResponseDto {

    private String explanation;
    private String rootCause;
    private List<String> recommendedFixes = new ArrayList<>();
    private String severityAssessment;
    private String diagnosticSource;

    public AiExplanationResponseDto() {
    }

    public AiExplanationResponseDto(String explanation, String rootCause,
                                    List<String> recommendedFixes, String severityAssessment) {
        this(explanation, rootCause, recommendedFixes, severityAssessment, null);
    }

    public AiExplanationResponseDto(String explanation, String rootCause,
                                    List<String> recommendedFixes, String severityAssessment,
                                    String diagnosticSource) {
        this.explanation = explanation;
        this.rootCause = rootCause;
        if (recommendedFixes != null) {
            this.recommendedFixes = recommendedFixes;
        }
        this.severityAssessment = severityAssessment;
        this.diagnosticSource = diagnosticSource;
    }

    // Getters and Setters

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public List<String> getRecommendedFixes() {
        return recommendedFixes;
    }

    public void setRecommendedFixes(List<String> recommendedFixes) {
        this.recommendedFixes = recommendedFixes;
    }

    public String getSeverityAssessment() {
        return severityAssessment;
    }

    public void setSeverityAssessment(String severityAssessment) {
        this.severityAssessment = severityAssessment;
    }

    public String getDiagnosticSource() {
        return diagnosticSource;
    }

    public void setDiagnosticSource(String diagnosticSource) {
        this.diagnosticSource = diagnosticSource;
    }
}
