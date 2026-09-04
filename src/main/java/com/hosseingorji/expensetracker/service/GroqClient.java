package com.hosseingorji.expensetracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around Groq's OpenAI-compatible chat completions endpoint.
 * Every failure mode (no key, HTTP error, unparseable body) returns an empty
 * string so callers can apply their own fallback instead of handling exceptions.
 */
@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    public GroqClient(
            RestTemplate restTemplate,
            @Value("${groq.api.key:}") String apiKey,
            @Value("${groq.api.url}") String apiUrl,
            @Value("${groq.api.model}") String model) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Sends a single user message and returns the assistant's reply text, or "" on any failure. */
    public String chat(String prompt, int maxTokens) {
        if (!isConfigured()) {
            log.warn("GROQ_API_KEY not set; skipping LLM call");
            return "";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0,
                "max_tokens", maxTokens,
                // Some Groq models are "reasoning" models that otherwise spend the
                // whole token budget thinking and return empty content.
                "reasoning_effort", "low"
        );

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(apiUrl, new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").path(0).path("message").path("content").asString().trim();
        } catch (Exception e) {
            log.warn("Groq call failed: {}", e.getMessage());
            return "";
        }
    }
}
