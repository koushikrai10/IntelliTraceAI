package com.intellitrace.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for external AI service integration.
 *
 * <p>Uses the Google Gemini REST API (generateContent endpoint).
 * Supply the API key via the {@code GEMINI_API_KEY} environment variable.
 * Never commit real keys into source control.
 *
 * <p>Selected model: {@code gemini-3.8-flash}
 * <ul>
 *   <li>Fast, cost-effective multimodal model from the Gemini generation.</li>
 *   <li>Suitable for structured text generation tasks such as log diagnostics.</li>
 *   <li>Override via {@code intellitrace.ai.model} property or {@code GEMINI_MODEL} env var.</li>
 * </ul>
 *
 * <p>REST endpoint pattern:
 * {@code POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}}
 */
@Configuration
@ConfigurationProperties(prefix = "intellitrace.ai")
public class AiConfig {

    /**
     * API key for the Google Gemini API.
     * Loaded from environment variable {@code GEMINI_API_KEY}.
     * Never hardcode or log this value.
     */
    private String apiKey;

    /**
     * Base URL of the Gemini generateContent REST API.
     * The model name and API key are appended at call time.
     */
    private String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models";

    /**
     * Gemini model identifier.
     * Default: {@code gemini-3.8-flash} — fast, general-purpose model.
     * Alternatives: {@code gemini-1.5-flash}, {@code gemini-1.5-pro}.
     */
    private String model = "gemini-3.8-flash";

    /**
     * Connection and response timeout in seconds.
     */
    private int timeoutSeconds = 30;

    /**
     * Sampling temperature for response generation (Gemini generationConfig).
     * Range 0.0–2.0. Lower values produce more deterministic output.
     */
    private double temperature = 0.2;

    /**
     * Maximum number of output tokens to request from Gemini (generationConfig.maxOutputTokens).
     */
    private int maxTokens = 1024;

    /**
     * Toggle flag to enable/disable external AI requests entirely.
     */
    private boolean enabled = true;

    // -------------------------------------------------------------------------
    // Derived helpers
    // -------------------------------------------------------------------------

    /**
     * Builds the Gemini generateContent REST URL for the configured model.
     * The API key is sent via the {@code x-goog-api-key} header for security,
     * keeping keys out of URL query parameters and server access logs.
     *
     * <p>Example: {@code https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent}
     */
    public String buildGeminiUrl() {
        if (apiUrl == null || apiUrl.isBlank()) {
            return "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
        }
        if (apiUrl.contains(":generateContent")) {
            return apiUrl;
        }
        String trimmed = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        return trimmed + "/" + model + ":generateContent";
    }

    /**
     * Returns {@code true} when AI is enabled and a non-blank API key is present.
     */
    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

