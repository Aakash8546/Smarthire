// backend/src/main/java/com/smarthire/service/MLServiceClient.java
package com.smarthire.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class MLServiceClient {
    @Value("${ml-service.url}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MLServiceClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public List<Map<String, Object>> rankCandidates(Long jobId, List<Map<String, Object>> candidates) {
        try {
            String url = mlServiceUrl + "/rank";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = new HashMap<>();
            request.put("job_id", jobId);
            request.put("candidates", candidates);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JsonNode.class);

            return objectMapper.convertValue(response.getBody(), List.class);
        } catch (Exception e) {
            log.error("Error calling ML service for ranking", e);
            return candidates; // Fallback: return original order
        }
    }

    public Map<String, Object> generateEmbedding(String text) {
        try {
            String url = mlServiceUrl + "/embed";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Collections.singletonMap("text", text);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JsonNode.class);

            return objectMapper.convertValue(response.getBody(), Map.class);
        } catch (Exception e) {
            log.error("Error generating embedding", e);
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> detectFraud(String resumeText) {
        try {
            String url = mlServiceUrl + "/fraud-check";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Collections.singletonMap("resume_text", resumeText);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JsonNode.class);

            return objectMapper.convertValue(response.getBody(), Map.class);
        } catch (Exception e) {
            log.error("Error detecting fraud", e);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("is_fraud", false);
            fallback.put("confidence", 0.0);
            return fallback;
        }
    }

    public Map<String, Object> detectSpam(String message) {
        try {
            String url = mlServiceUrl + "/detect-spam";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Collections.singletonMap("message", message);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JsonNode.class);

            return objectMapper.convertValue(response.getBody(), Map.class);
        } catch (Exception e) {
            log.error("Error detecting spam", e);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("is_spam", false);
            fallback.put("confidence", 0.0);
            fallback.put("reason", "ML service unavailable");
            return fallback;
        }
    }
}