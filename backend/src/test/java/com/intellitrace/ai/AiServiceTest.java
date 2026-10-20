package com.intellitrace.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrace.dto.AiExplanationRequestDto;
import com.intellitrace.dto.AiExplanationResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiService Unit Tests (Google Gemini & Heuristic Fallback)")
class AiServiceTest {

    private AiConfig aiConfig;
    private ObjectMapper objectMapper;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    @BeforeEach
    void setUp() {
        aiConfig = new AiConfig();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should use heuristic engine when GEMINI_API_KEY is not configured")
    void testExplainErrorWithHeuristicWhenNoApiKey() {
        aiConfig.setApiKey(null);
        AiService aiService = new AiService(aiConfig, objectMapper, mockHttpClient);

        AiExplanationRequestDto request = new AiExplanationRequestDto(
                "NullPointerException: user not found",
                "Cannot invoke find() on null reference",
                "at com.example.Service.run(Service.java:10)",
                "Production web server"
        );

        AiExplanationResponseDto response = aiService.explainError(request);

        assertNotNull(response);
        assertNotNull(response.getExplanation());
        assertTrue(response.getExplanation().contains("NullPointerException"));
        assertEquals("HIGH", response.getSeverityAssessment());
        assertEquals("HEURISTIC", response.getDiagnosticSource());
        assertFalse(response.getRecommendedFixes().isEmpty());
    }

    @Test
    @DisplayName("Should generate SQL heuristics for database related errors")
    void testHeuristicSqlException() {
        aiConfig.setApiKey("");
        AiService aiService = new AiService(aiConfig, objectMapper, mockHttpClient);

        AiExplanationRequestDto request = new AiExplanationRequestDto(
                "SQLException: Connection refused to database",
                "Connection timeout after 30000ms",
                null,
                null
        );

        AiExplanationResponseDto response = aiService.explainError(request);
        assertEquals("CRITICAL", response.getSeverityAssessment());
        assertEquals("HEURISTIC", response.getDiagnosticSource());
        assertTrue(response.getRootCause().toLowerCase().contains("database"));
    }

    @Test
    @DisplayName("Should generate timeout heuristics for timeout related errors")
    void testHeuristicTimeoutException() {
        aiConfig.setApiKey("");
        AiService aiService = new AiService(aiConfig, objectMapper, mockHttpClient);

        AiExplanationRequestDto request = new AiExplanationRequestDto(
                "SocketTimeoutException: Read timed out",
                "Read timed out waiting for remote host",
                null,
                null
        );

        AiExplanationResponseDto response = aiService.explainError(request);
        assertEquals("HIGH", response.getSeverityAssessment());
        assertEquals("HEURISTIC", response.getDiagnosticSource());
        assertTrue(response.getExplanation().toLowerCase().contains("timeout"));
    }

    @Test
    @DisplayName("Should generate memory heuristics for OutOfMemoryError")
    void testHeuristicOutOfMemoryError() {
        aiConfig.setApiKey("");
        AiService aiService = new AiService(aiConfig, objectMapper, mockHttpClient);

        AiExplanationRequestDto request = new AiExplanationRequestDto(
                "OutOfMemoryError: Java heap space",
                "Heap limit exceeded during batch processing",
                null,
                null
        );

        AiExplanationResponseDto response = aiService.explainError(request);
        assertEquals("CRITICAL", response.getSeverityAssessment());
        assertEquals("HEURISTIC", response.getDiagnosticSource());
        assertTrue(response.getRootCause().toLowerCase().contains("memory"));
    }

    @Test
    @DisplayName("Should reject blank error pattern with IllegalArgumentException")
    void testExplainErrorValidation() {
        AiService aiService = new AiService(aiConfig, objectMapper, mockHttpClient);
        AiExplanationRequestDto request = new AiExplanationRequestDto("", "msg", null, null);

        assertThrows(IllegalArgumentException.class, () -> aiService.explainError(request));
    }

    @Test
    @DisplayName("Should configure gemini-3.8-flash as default model and build correct endpoint URL")
    void testDefaultModelAndUrlConfiguration() {
        AiConfig config = new AiConfig();
        assertEquals("gemini-3.8-flash", config.getModel());
        assertEquals(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent",
                config.buildGeminiUrl()
        );

        // Verify custom model override builds expected URL pattern
        config.setModel("gemini-1.5-pro");
        assertEquals(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent",
                config.buildGeminiUrl()
        );
    }

    @Test
    @DisplayName("Should parse Google Gemini generateContent JSON response successfully with gemini-3.8-flash")
    @SuppressWarnings("unchecked")
    void testCallExternalGeminiSuccess() throws Exception {
        aiConfig.setApiKey("test-gemini-key");
        // default model is gemini-3.8-flash

        AiService aiService = new AiService(aiConfig, objectMapper, mockHttpClient);

        String geminiJsonResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"explanation\\":\\"Gemini Diagnostic Analysis: Null pointer during order lookup\\",\\"rootCause\\":\\"Order entity was missing from database resulting in null reference\\",\\"recommendedFixes\\":[\\"Add Optional check before accessing entity\\",\\"Validate incoming order ID\\"],\\"severityAssessment\\":\\"HIGH\\"}"
                          }
                        ],
                        "role": "model"
                      },
                      "finishReason": "STOP",
                      "index": 0
                    }
                  ],
                  "usageMetadata": {
                    "promptTokenCount": 85,
                    "candidatesTokenCount": 64,
                    "totalTokenCount": 149
                  }
                }
                """;

        org.mockito.ArgumentCaptor<HttpRequest> requestCaptor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(geminiJsonResponse);
        when(mockHttpClient.send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        AiExplanationRequestDto request = new AiExplanationRequestDto(
                "NullPointerException in OrderService", "sample message", "stacktrace", "test context"
        );

        AiExplanationResponseDto response = aiService.explainError(request);

        assertNotNull(response);
        assertEquals("Gemini Diagnostic Analysis: Null pointer during order lookup", response.getExplanation());
        assertEquals("Order entity was missing from database resulting in null reference", response.getRootCause());
        assertEquals(2, response.getRecommendedFixes().size());
        assertEquals("HIGH", response.getSeverityAssessment());
        assertEquals("GEMINI", response.getDiagnosticSource());

        // Verify request URL and x-goog-api-key authentication header
        HttpRequest sentRequest = requestCaptor.getValue();
        assertEquals(
                URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"),
                sentRequest.uri()
        );
        assertEquals("test-gemini-key", sentRequest.headers().firstValue("x-goog-api-key").orElse(null));
        assertEquals("application/json", sentRequest.headers().firstValue("Content-Type").orElse(null));
    }

    @Test
    @DisplayName("Should parse Gemini response when output is wrapped with markdown code fences")
    @SuppressWarnings("unchecked")
    void testGeminiResponseWithMarkdownFences() throws Exception {
        aiConfig.setApiKey("test-gemini-key");
        AiService aiService = new AiService(aiConfig, objectMapper, mockHttpClient);

        String geminiJsonResponseWithFences = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "```json\\n{\\"explanation\\":\\"Analysis inside code fence\\",\\"rootCause\\":\\"Service timeout\\",\\"recommendedFixes\\":[\\"Increase timeout\\"],\\"severityAssessment\\":\\"HIGH\\"}\\n```"
                          }
                        ],
                        "role": "model"
                      },
                      "finishReason": "STOP",
                      "index": 0
                    }
                  ]
                }
                """;

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(geminiJsonResponseWithFences);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        AiExplanationRequestDto request = new AiExplanationRequestDto(
                "TimeoutException in HttpClient", "timeout", null, null
        );

        AiExplanationResponseDto response = aiService.explainError(request);

        assertNotNull(response);
        assertEquals("Analysis inside code fence", response.getExplanation());
        assertEquals("Service timeout", response.getRootCause());
        assertEquals("GEMINI", response.getDiagnosticSource());
    }

    @Test
    @DisplayName("Should gracefully fall back to heuristic when Gemini returns HTTP 500 error")
    @SuppressWarnings("unchecked")
    void testCallExternalGeminiFallbackOnHttp500() throws Exception {
        aiConfig.setApiKey("test-gemini-key");
        AiService aiService = new AiService(aiConfig, objectMapper, mockHttpClient);

        aiService.setInitialBackoffMs(1L);

        String geminiErrorResponse = """
                {
                  "error": {
                    "code": 500,
                    "message": "Internal error occurred in Gemini inference service",
                    "status": "INTERNAL"
                  }
                }
                """;

        when(mockHttpResponse.statusCode()).thenReturn(500);
        when(mockHttpResponse.body()).thenReturn(geminiErrorResponse);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        AiExplanationRequestDto request = new AiExplanationRequestDto(
                "OutOfMemoryError: Java heap space", "heap exceeded", null, null
        );

        AiExplanationResponseDto response = aiService.explainError(request);

        assertNotNull(response);
        assertTrue(response.getExplanation().contains("External AI unavailable"));
        assertEquals("CRITICAL", response.getSeverityAssessment());
        assertEquals("HEURISTIC_FALLBACK", response.getDiagnosticSource());
    }

    @Test
    @DisplayName("Should gracefully fall back to heuristic when Gemini API returns HTTP 429 rate limit")
    @SuppressWarnings("unchecked")
    void testCallExternalGeminiFallbackOnRateLimit() throws Exception {
        aiConfig.setApiKey("test-gemini-key");
        AiService aiService = new AiService(aiConfig, objectMapper, mockHttpClient);
        aiService.setInitialBackoffMs(1L);

        String rateLimitJson = """
                {
                  "error": {
                    "code": 429,
                    "message": "Resource has been exhausted (e.g. check quota)",
                    "status": "RESOURCE_EXHAUSTED"
                  }
                }
                """;

        when(mockHttpResponse.statusCode()).thenReturn(429);
        when(mockHttpResponse.body()).thenReturn(rateLimitJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        AiExplanationRequestDto request = new AiExplanationRequestDto(
                "NullPointerException in AuthController", "null user", null, null
        );

        AiExplanationResponseDto response = aiService.explainError(request);

        assertNotNull(response);
        assertTrue(response.getExplanation().contains("External AI unavailable"));
        assertEquals("HEURISTIC_FALLBACK", response.getDiagnosticSource());
        assertEquals("HIGH", response.getSeverityAssessment());
    }

    @Test
    @DisplayName("Should succeed on retry when Gemini initially returns HTTP 503 UNAVAILABLE")
    @SuppressWarnings("unchecked")
    void testCallExternalGeminiSuccessfulRetryAfter503() throws Exception {
        aiConfig.setApiKey("test-gemini-key");
        AiService aiService = new AiService(aiConfig, objectMapper, mockHttpClient);
        aiService.setInitialBackoffMs(1L);

        HttpResponse<String> response503 = mock(HttpResponse.class);
        when(response503.statusCode()).thenReturn(503);

        HttpResponse<String> response200 = mock(HttpResponse.class);
        when(response200.statusCode()).thenReturn(200);
        String successJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"explanation\\":\\"Recovered after 503 retry\\",\\"rootCause\\":\\"High concurrency spike\\",\\"recommendedFixes\\":[\\"Enable connection pooling\\"],\\"severityAssessment\\":\\"MEDIUM\\"}"
                          }
                        ],
                        "role": "model"
                      },
                      "finishReason": "STOP",
                      "index": 0
                    }
                  ]
                }
                """;
        when(response200.body()).thenReturn(successJson);

        // First attempt returns 503, second attempt (first retry) returns 200
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response503)
                .thenReturn(response200);

        AiExplanationRequestDto request = new AiExplanationRequestDto(
                "ConnectionException in Pool", "pool timeout", null, null
        );

        AiExplanationResponseDto response = aiService.explainError(request);

        assertNotNull(response);
        assertEquals("Recovered after 503 retry", response.getExplanation());
        assertEquals("High concurrency spike", response.getRootCause());
        assertEquals("GEMINI", response.getDiagnosticSource());

        // Verify exactly 2 calls were made (1 initial + 1 retry)
        verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("Should fall back to heuristic when all 2 retries fail on persistent HTTP 503")
    @SuppressWarnings("unchecked")
    void testCallExternalGeminiFallbackAfterAll503RetriesFail() throws Exception {
        aiConfig.setApiKey("test-gemini-key");
        AiService aiService = new AiService(aiConfig, objectMapper, mockHttpClient);
        aiService.setInitialBackoffMs(1L);

        when(mockHttpResponse.statusCode()).thenReturn(503);
        when(mockHttpResponse.body()).thenReturn("{\"error\":{\"code\":503,\"message\":\"The model is overloaded. Please try again later.\",\"status\":\"UNAVAILABLE\"}}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        AiExplanationRequestDto request = new AiExplanationRequestDto(
                "NullPointerException: customer record not loaded", "missing customer", null, null
        );

        AiExplanationResponseDto response = aiService.explainError(request);

        assertNotNull(response);
        assertTrue(response.getExplanation().contains("External AI unavailable"));
        assertTrue(response.getExplanation().contains("NullPointerException"));
        assertEquals("HEURISTIC_FALLBACK", response.getDiagnosticSource());

        // Verify exactly 3 calls were made (1 initial + 2 retries = 3 total attempts)
        verify(mockHttpClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("Should not retry non-transient client errors such as HTTP 401 or 404")
    @SuppressWarnings("unchecked")
    void testCallExternalGeminiNoRetryOnClientError() throws Exception {
        aiConfig.setApiKey("invalid-key");
        AiService aiService = new AiService(aiConfig, objectMapper, mockHttpClient);
        aiService.setInitialBackoffMs(1L);

        when(mockHttpResponse.statusCode()).thenReturn(401);
        when(mockHttpResponse.body()).thenReturn("{\"error\":{\"code\":401,\"message\":\"API key not valid\",\"status\":\"UNAUTHENTICATED\"}}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        AiExplanationRequestDto request = new AiExplanationRequestDto(
                "NullPointerException in UserService", "null user", null, null
        );

        AiExplanationResponseDto response = aiService.explainError(request);

        assertNotNull(response);
        assertTrue(response.getExplanation().contains("External AI unavailable"));
        assertEquals("HEURISTIC_FALLBACK", response.getDiagnosticSource());

        // Verify only 1 attempt was made (no retries for 401)
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("Should gracefully fall back to heuristic when Gemini API encounters a request timeout")
    @SuppressWarnings("unchecked")
    void testCallExternalGeminiFallbackOnTimeout() throws Exception {
        aiConfig.setApiKey("test-gemini-key");
        AiService aiService = new AiService(aiConfig, objectMapper, mockHttpClient);
        aiService.setInitialBackoffMs(1L);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpTimeoutException("Connection timed out after 30 seconds"));

        AiExplanationRequestDto request = new AiExplanationRequestDto(
                "SQLException: Connection refused", "DB down", null, null
        );

        AiExplanationResponseDto response = aiService.explainError(request);

        assertNotNull(response);
        assertTrue(response.getExplanation().contains("External AI unavailable"));
        assertEquals("HEURISTIC_FALLBACK", response.getDiagnosticSource());
        assertEquals("CRITICAL", response.getSeverityAssessment());
    }

    @Test
    @DisplayName("Should fall back to heuristic when Gemini returns empty candidates list")
    @SuppressWarnings("unchecked")
    void testCallExternalGeminiFallbackOnEmptyCandidates() throws Exception {
        aiConfig.setApiKey("test-gemini-key");
        AiService aiService = new AiService(aiConfig, objectMapper, mockHttpClient);

        String emptyCandidatesJson = "{\"candidates\": []}";

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(emptyCandidatesJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        AiExplanationRequestDto request = new AiExplanationRequestDto(
                "NullPointerException", "sample", null, null
        );

        AiExplanationResponseDto response = aiService.explainError(request);

        assertNotNull(response);
        assertTrue(response.getExplanation().contains("External AI unavailable"));
        assertEquals("HEURISTIC_FALLBACK", response.getDiagnosticSource());
    }

    @Test
    @DisplayName("Should encapsulate raw text when Gemini response is not valid JSON")
    @SuppressWarnings("unchecked")
    void testGeminiRawTextResponseEncapsulation() throws Exception {
        aiConfig.setApiKey("test-gemini-key");
        AiService aiService = new AiService(aiConfig, objectMapper, mockHttpClient);

        String rawTextResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "The NullPointerException occurred because the cache returned null for this key."
                          }
                        ],
                        "role": "model"
                      },
                      "finishReason": "STOP",
                      "index": 0
                    }
                  ]
                }
                """;

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(rawTextResponse);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        AiExplanationRequestDto request = new AiExplanationRequestDto(
                "NullPointerException", "sample", null, null
        );

        AiExplanationResponseDto response = aiService.explainError(request);

        assertNotNull(response);
        assertTrue(response.getExplanation().contains("The NullPointerException occurred"));
        assertEquals("GEMINI", response.getDiagnosticSource());
    }
}

