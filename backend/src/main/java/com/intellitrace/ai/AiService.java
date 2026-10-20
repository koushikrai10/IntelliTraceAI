package com.intellitrace.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellitrace.dto.AiExplanationRequestDto;
import com.intellitrace.dto.AiExplanationResponseDto;
import com.intellitrace.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for invoking the Google Gemini REST API (generateContent) to produce
 * diagnostic explanations, root-cause analyses, and actionable remediation advice.
 *
 * <p>Includes deterministic heuristic fallback when credentials are not configured,
 * when the external service is unreachable, times out, or encounters errors.
 */
@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @org.springframework.beans.factory.annotation.Autowired
    public AiService(AiConfig aiConfig, ObjectMapper objectMapper) {
        this.aiConfig = aiConfig;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Internal constructor enabling custom HttpClient injection for testing.
     */
    public AiService(AiConfig aiConfig, ObjectMapper objectMapper, HttpClient httpClient) {
        this.aiConfig = aiConfig;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * Explains a specific error pattern or stack trace.
     *
     * @param request error pattern, stack trace, and context
     * @return structured explanation and remediation recommendations
     */
    public AiExplanationResponseDto explainError(AiExplanationRequestDto request) {
        if (request == null || request.getErrorPattern() == null || request.getErrorPattern().isBlank()) {
            throw new IllegalArgumentException("Error pattern must be provided for AI explanation");
        }

        // If AI is not configured or disabled, use deterministic heuristic engine
        if (!aiConfig.isConfigured()) {
            logger.info("Gemini AI credentials not configured. Generating heuristic explanation for pattern: {}", request.getErrorPattern());
            AiExplanationResponseDto heuristicResponse = generateHeuristicExplanation(request);
            heuristicResponse.setDiagnosticSource("HEURISTIC");
            return heuristicResponse;
        }

        try {
            return callExternalAi(request);
        } catch (Exception e) {
            logger.warn("External Gemini AI call failed ({}). Falling back to heuristic explanation.", e.getMessage());
            AiExplanationResponseDto fallback = generateHeuristicExplanation(request);
            fallback.setExplanation("[Notice: External AI unavailable (Google Gemini) - generated via local heuristics]\n\n" + fallback.getExplanation());
            fallback.setDiagnosticSource("HEURISTIC_FALLBACK");
            return fallback;
        }
    }

    public static final int MAX_RETRIES = 2;
    private long initialBackoffMs = 500L;
    private double backoffMultiplier = 2.0;

    public void setInitialBackoffMs(long initialBackoffMs) {
        this.initialBackoffMs = initialBackoffMs;
    }

    /**
     * Pauses the current thread for the specified duration.
     * Can be overridden or timed in tests.
     */
    protected void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    /**
     * Identifies transient HTTP status codes eligible for automatic retry.
     */
    private boolean isTransientError(int statusCode) {
        return statusCode == 503 || statusCode == 429 || statusCode == 500;
    }

    /**
     * Calls the Google Gemini generateContent REST endpoint with retry support for transient errors.
     */
    protected AiExplanationResponseDto callExternalAi(AiExplanationRequestDto request) {
        String prompt = buildPrompt(request);

        String requestBody;
        try {
            ObjectNode rootNode = objectMapper.createObjectNode();

            // systemInstruction
            ObjectNode systemInstruction = rootNode.putObject("systemInstruction");
            ArrayNode systemParts = systemInstruction.putArray("parts");
            systemParts.addObject().put("text",
                    "You are IntelliTrace AI, an expert software diagnostics engineer. "
                            + "Analyze application logs and stack traces. "
                            + "Always reply with valid JSON containing the following string/array fields: "
                            + "\"explanation\" (string), \"rootCause\" (string), "
                            + "\"recommendedFixes\" (array of strings), \"severityAssessment\" (string)."
            );

            // contents
            ArrayNode contentsNode = rootNode.putArray("contents");
            ObjectNode userContent = contentsNode.addObject();
            userContent.put("role", "user");
            ArrayNode userParts = userContent.putArray("parts");
            userParts.addObject().put("text", prompt);

            // generationConfig
            ObjectNode genConfig = rootNode.putObject("generationConfig");
            genConfig.put("temperature", aiConfig.getTemperature());
            genConfig.put("maxOutputTokens", aiConfig.getMaxTokens());
            genConfig.put("responseMimeType", "application/json");

            requestBody = objectMapper.writeValueAsString(rootNode);
        } catch (IOException e) {
            throw new AiServiceException("Failed to serialize Gemini request payload: " + e.getMessage(), e);
        }

        String url = aiConfig.buildGeminiUrl();

        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds()))
                .header("Content-Type", "application/json");

        if (aiConfig.getApiKey() != null && !aiConfig.getApiKey().isBlank()) {
            httpRequestBuilder.header("x-goog-api-key", aiConfig.getApiKey());
        }

        HttpRequest httpRequest = httpRequestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        int maxAttempts = 1 + MAX_RETRIES; // 1 initial attempt + max 2 retries
        long backoffMs = initialBackoffMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                int statusCode = response.statusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    AiExplanationResponseDto result = parseGeminiResponse(response.body());
                    result.setDiagnosticSource("GEMINI");
                    return result;
                }

                // Check for transient failure (503, 429, 500)
                if (isTransientError(statusCode)) {
                    if (attempt < maxAttempts) {
                        logger.warn("Transient Gemini API error (HTTP {}). Retrying attempt {} of {} in {}ms...",
                                statusCode, attempt, MAX_RETRIES, backoffMs);
                        try {
                            sleep(backoffMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new AiServiceException("Gemini retry was interrupted", ie);
                        }
                        backoffMs = (long) (backoffMs * backoffMultiplier);
                        continue;
                    } else {
                        logger.warn("Transient Gemini API error (HTTP {}) persisted after {} attempts.", statusCode, maxAttempts);
                    }
                }

                // Non-retryable error (e.g., 401, 403, 404) or all retries exhausted
                handleHttpError(statusCode, response.body());

            } catch (HttpTimeoutException e) {
                if (attempt < maxAttempts) {
                    logger.warn("Timeout communicating with Gemini endpoint. Retrying attempt {} of {} in {}ms...",
                            attempt, MAX_RETRIES, backoffMs);
                    try {
                        sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AiServiceException("Gemini retry was interrupted", ie);
                    }
                    backoffMs = (long) (backoffMs * backoffMultiplier);
                    continue;
                }
                logger.warn("Timeout communicating with Gemini endpoint: {}", e.getMessage());
                throw new AiServiceException("Timeout waiting for Gemini response (" + aiConfig.getTimeoutSeconds() + "s)", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AiServiceException("AI request thread was interrupted", e);
            } catch (IOException e) {
                if (attempt < maxAttempts) {
                    logger.warn("I/O error communicating with Gemini endpoint ({}). Retrying attempt {} of {} in {}ms...",
                            e.getMessage(), attempt, MAX_RETRIES, backoffMs);
                    try {
                        sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AiServiceException("Gemini retry was interrupted", ie);
                    }
                    backoffMs = (long) (backoffMs * backoffMultiplier);
                    continue;
                }
                throw new AiServiceException("Failed to communicate with Gemini AI endpoint: " + e.getMessage(), e);
            }
        }

        throw new AiServiceException("Gemini call failed after " + maxAttempts + " attempts");
    }

    /**
     * Handles HTTP error responses from the Gemini API securely without exposing API keys.
     */
    private void handleHttpError(int statusCode, String responseBody) {
        String detail = "";
        try {
            if (responseBody != null && !responseBody.isBlank()) {
                JsonNode errJson = objectMapper.readTree(responseBody);
                JsonNode errorObj = errJson.path("error");
                if (!errorObj.isMissingNode()) {
                    String message = errorObj.path("message").asText("");
                    String status = errorObj.path("status").asText("");
                    if (!message.isBlank()) {
                        detail = status + ": " + message;
                    }
                }
            }
        } catch (Exception ignored) {
            // Avoid logging raw response body if it might contain unverified content
        }

        if (statusCode == 503) {
            logger.warn("Google Gemini API unavailable due to high demand (HTTP 503)");
            throw new AiServiceException("Gemini API temporarily unavailable due to high demand (HTTP 503)" + (detail.isBlank() ? "" : ": " + detail));
        } else if (statusCode == 429) {
            logger.warn("Google Gemini API rate limit exceeded (HTTP 429)");
            throw new AiServiceException("Gemini API rate limit exceeded (HTTP 429)" + (detail.isBlank() ? "" : ": " + detail));
        } else if (statusCode == 401 || statusCode == 403) {
            logger.warn("Google Gemini API authentication failed (HTTP {})", statusCode);
            throw new AiServiceException("Gemini API authentication failed (HTTP " + statusCode + ")");
        } else if (statusCode == 404) {
            logger.warn("Google Gemini API model not found (HTTP 404)");
            throw new AiServiceException("Gemini API model not found (HTTP 404)" + (detail.isBlank() ? "" : ": " + detail));
        } else {
            logger.error("Google Gemini API returned HTTP status {}{}", statusCode, detail.isBlank() ? "" : ": " + detail);
            throw new AiServiceException("Gemini API error (HTTP " + statusCode + ")" + (detail.isBlank() ? "" : ": " + detail));
        }
    }

    /**
     * Parses the response from Google Gemini generateContent REST API.
     */
    protected AiExplanationResponseDto parseGeminiResponse(String responseJson) throws IOException {
        if (responseJson == null || responseJson.isBlank()) {
            throw new AiServiceException("Empty response received from Gemini provider");
        }

        JsonNode rootNode = objectMapper.readTree(responseJson);

        // Check for error block in 200 responses if any
        if (rootNode.has("error")) {
            String errMsg = rootNode.path("error").path("message").asText("Unknown Gemini error");
            throw new AiServiceException("Gemini provider returned error: " + errMsg);
        }

        JsonNode candidates = rootNode.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new AiServiceException("No candidates returned from Gemini provider");
        }

        JsonNode firstCandidate = candidates.get(0);
        JsonNode parts = firstCandidate.path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            String finishReason = firstCandidate.path("finishReason").asText("UNKNOWN");
            throw new AiServiceException("No content parts in Gemini response candidate (finishReason: " + finishReason + ")");
        }

        String rawContent = parts.get(0).path("text").asText();
        if (rawContent == null || rawContent.isBlank()) {
            throw new AiServiceException("Empty text content received from Gemini provider");
        }

        // Clean JSON markdown blocks if present (```json ... ```)
        String cleanedJson = rawContent.trim();
        if (cleanedJson.startsWith("```json")) {
            cleanedJson = cleanedJson.substring(7);
        } else if (cleanedJson.startsWith("```")) {
            cleanedJson = cleanedJson.substring(3);
        }
        if (cleanedJson.endsWith("```")) {
            cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
        }
        cleanedJson = cleanedJson.trim();

        try {
            JsonNode structured = objectMapper.readTree(cleanedJson);
            String explanation = structured.path("explanation").asText("Detailed analysis completed.");
            String rootCause = structured.path("rootCause").asText("Unable to determine root cause.");
            String severity = structured.path("severityAssessment").asText("MEDIUM");

            List<String> fixes = new ArrayList<>();
            JsonNode fixesNode = structured.path("recommendedFixes");
            if (fixesNode.isArray()) {
                for (JsonNode fixNode : fixesNode) {
                    fixes.add(fixNode.asText());
                }
            }

            AiExplanationResponseDto response = new AiExplanationResponseDto(explanation, rootCause, fixes, severity);
            response.setDiagnosticSource("GEMINI");
            return response;
        } catch (Exception e) {
            // If the model didn't return valid structured JSON, encapsulate raw text safely
            AiExplanationResponseDto fallbackResponse = new AiExplanationResponseDto(
                    rawContent,
                    "Derived from log message context",
                    List.of("Review the diagnostic message above for suggested action items."),
                    "MEDIUM"
            );
            fallbackResponse.setDiagnosticSource("GEMINI");
            return fallbackResponse;
        }
    }

    /**
     * Constructs the contextual prompt sent to Gemini.
     * Only relevant error group data is included: pattern, sample message, stack trace, and context.
     * Never sends the full raw log file.
     */
    private String buildPrompt(AiExplanationRequestDto request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Please diagnose the following error:\n");
        sb.append("Error Pattern: ").append(request.getErrorPattern()).append("\n");
        if (request.getSampleMessage() != null && !request.getSampleMessage().isBlank()) {
            sb.append("Sample Message: ").append(request.getSampleMessage()).append("\n");
        }
        if (request.getStackTrace() != null && !request.getStackTrace().isBlank()) {
            sb.append("Stack Trace:\n").append(request.getStackTrace()).append("\n");
        }
        if (request.getContext() != null && !request.getContext().isBlank()) {
            sb.append("Application Context: ").append(request.getContext()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Deterministic heuristic diagnostic generator used when no external AI is configured or reachable.
     */
    public AiExplanationResponseDto generateHeuristicExplanation(AiExplanationRequestDto request) {
        String pattern = request.getErrorPattern() != null ? request.getErrorPattern() : "";
        String sample = request.getSampleMessage() != null ? request.getSampleMessage() : "";
        String combined = (pattern + " " + sample).toLowerCase();

        String explanation;
        String rootCause;
        List<String> fixes = new ArrayList<>();
        String severity;

        if (combined.contains("nullpointerexception")) {
            explanation = "A NullPointerException occurred because an object reference evaluated to null while invoking a method or accessing a property.";
            rootCause = "Dereferencing an uninitialized or missing variable or object reference.";
            fixes.add("Inspect where the object reference is initialized and verify it is not null before method invocation.");
            fixes.add("Employ Java Optional<T> or Objects.requireNonNull() to safeguard against null pointers.");
            fixes.add("Verify incoming API payloads or database query results that populate this object.");
            severity = "HIGH";
        } else if (combined.contains("sqlexception") || combined.contains("jdbc") || combined.contains("connection refused")) {
            explanation = "A database connectivity or SQL query execution failure was encountered by the application.";
            rootCause = "Database unreachable, invalid SQL syntax, exhausted connection pool, or stale network credentials.";
            fixes.add("Verify database service availability, network connectivity, and connection credentials.");
            fixes.add("Check datasource pool settings (e.g. HikariCP maximumPoolSize, connectionTimeout).");
            fixes.add("Examine the SQL dialect and table schema to ensure compatibility with query syntax.");
            severity = "CRITICAL";
        } else if (combined.contains("timeoutexception") || combined.contains("sockettimeoutexception") || combined.contains("read timed out")) {
            explanation = "An outbound network or downstream service call exceeded the configured execution timeout threshold.";
            rootCause = "Downstream service latency, network degradation, or overly restrictive client timeouts.";
            fixes.add("Check downstream service health, load averages, and latency metrics.");
            fixes.add("Increase client read/connect timeouts if downstream processing legitimately takes longer.");
            fixes.add("Implement circuit breaker (e.g. Resilience4j) and fallback mechanisms.");
            severity = "HIGH";
        } else if (combined.contains("outofmemoryerror") || combined.contains("heap space")) {
            explanation = "The Java Virtual Machine ran out of heap memory while trying to allocate an object.";
            rootCause = "Memory leak, excessive caching, or insufficient JVM max heap (-Xmx) allocation.";
            fixes.add("Capture and inspect heap dumps (-XX:+HeapDumpOnOutOfMemoryError) with Eclipse MAT or VisualVM.");
            fixes.add("Increase JVM heap space allocating more memory using -Xmx.");
            fixes.add("Inspect unbounded data collections, static maps, or unclosed streaming resources.");
            severity = "CRITICAL";
        } else {
            explanation = "Diagnostic analysis completed for pattern: " + pattern;
            rootCause = "Execution anomaly identified in application logic or dependent services.";
            fixes.add("Examine the stack trace frames to pinpoint the originating class and line number.");
            fixes.add("Check relevant service logs immediately before and after this event for correlated errors.");
            fixes.add("Add defensive validations and logging guards around the failing operation.");
            severity = "MEDIUM";
        }

        AiExplanationResponseDto response = new AiExplanationResponseDto(explanation, rootCause, fixes, severity);
        response.setDiagnosticSource("HEURISTIC");
        return response;
    }
}

