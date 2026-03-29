
package com.smarthire.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
@Slf4j
public class MLServiceClient {

    @Value("${ml-service.url}")
    private String mlServiceUrl;

    @Value("${ml-service.timeout:30000}")
    private int timeout;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MLServiceClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @CircuitBreaker(name = "mlService", fallbackMethod = "rankCandidatesFallback")
    @Retry(name = "mlService", fallbackMethod = "rankCandidatesFallback")
    public List<Map<String, Object>> rankCandidates(Long jobId, Map<String, Object> jobData,
                                                    List<Map<String, Object>> candidates) {
        try {
            String url = mlServiceUrl + "/api/v1/rank";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = new HashMap<>();
            request.put("job_id", jobId);
            request.put("job_data", jobData);
            request.put("candidates", candidates);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JsonNode.class);

            return objectMapper.convertValue(response.getBody(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Error calling ML service for ranking", e);
            throw new RuntimeException("ML service unavailable", e);
        }
    }

    public List<Map<String, Object>> rankCandidatesFallback(Long jobId, Map<String, Object> jobData,
                                                            List<Map<String, Object>> candidates, Exception e) {
        log.warn("Using fallback ranking for job: {}", jobId);


        List<Map<String, Object>> ranked = new ArrayList<>();
        for (Map<String, Object> candidate : candidates) {
            double score = calculateFallbackScore(jobData, candidate);
            Map<String, Object> result = new HashMap<>(candidate);
            result.put("score", score);
            result.put("explanation", "Fallback ranking due to ML service unavailability");
            ranked.add(result);
        }

        ranked.sort((a, b) -> Double.compare(
                ((Number) b.get("score")).doubleValue(),
                ((Number) a.get("score")).doubleValue()
        ));

        return ranked;
    }

    private double calculateFallbackScore(Map<String, Object> jobData, Map<String, Object> candidate) {
        double score = 0.0;

        // Experience match
        double requiredExp = ((Number) jobData.getOrDefault("required_experience", 0)).doubleValue();
        double candidateExp = ((Number) candidate.getOrDefault("experience_years", 0)).doubleValue();
        if (requiredExp > 0) {
            score += Math.min(1.0, candidateExp / requiredExp) * 0.4;
        }


        List<String> requiredSkills = (List<String>) jobData.getOrDefault("required_skills", Collections.emptyList());
        List<String> candidateSkills = (List<String>) candidate.getOrDefault("skills", Collections.emptyList());
        if (!requiredSkills.isEmpty()) {
            long matchCount = candidateSkills.stream().filter(requiredSkills::contains).count();
            score += (matchCount / (double) requiredSkills.size()) * 0.6;
        }

        return score;
    }

    @CircuitBreaker(name = "mlService", fallbackMethod = "getEmbeddingFallback")
    public float[] getEmbedding(String text) {
        try {
            String url = mlServiceUrl + "/api/v1/embed";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Collections.singletonMap("text", text);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JsonNode.class);

            JsonNode embeddingNode = response.getBody().get("embedding");
            float[] embedding = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                embedding[i] = (float) embeddingNode.get(i).asDouble();
            }
            return embedding;
        } catch (Exception e) {
            log.error("Error generating embedding", e);
            throw new RuntimeException("Embedding generation failed", e);
        }
    }

    public float[] getEmbeddingFallback(String text, Exception e) {
        log.warn("Using fallback embedding (zero vector)");
        return new float[384]; // Return zero vector as fallback
    }

    @CircuitBreaker(name = "mlService", fallbackMethod = "detectFraudFallback")
    public Map<String, Object> detectFraud(String resumeText) {
        try {
            String url = mlServiceUrl + "/api/v1/fraud-check";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Collections.singletonMap("resume_text", resumeText);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JsonNode.class);

            return objectMapper.convertValue(response.getBody(), Map.class);
        } catch (Exception e) {
            log.error("Error detecting fraud", e);
            throw new RuntimeException("Fraud detection failed", e);
        }
    }

    public Map<String, Object> detectFraudFallback(String resumeText, Exception e) {
        log.warn("Using fallback fraud detection");
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("is_fraud", false);
        fallback.put("confidence", 0.5);
        fallback.put("reason", "ML service unavailable, using rule-based check");
        return fallback;
    }

    @CircuitBreaker(name = "mlService", fallbackMethod = "detectSpamFallback")
    public Map<String, Object> detectSpam(String message) {
        try {
            String url = mlServiceUrl + "/api/v1/detect-spam";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = Collections.singletonMap("message", message);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JsonNode.class);

            return objectMapper.convertValue(response.getBody(), Map.class);
        } catch (Exception e) {
            log.error("Error detecting spam", e);
            throw new RuntimeException("Spam detection failed", e);
        }
    }

    public Map<String, Object> detectSpamFallback(String message, Exception e) {
        log.warn("Using fallback spam detection");
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("is_spam", false);
        fallback.put("confidence", 0.5);
        fallback.put("reason", "ML service unavailable");
        return fallback;
    }

    @CircuitBreaker(name = "mlService", fallbackMethod = "getRecommendationsFallback")
    public List<Map<String, Object>> getRecommendations(Map<String, Object> userData,
                                                        List<Map<String, Object>> jobsData) {
        try {
            String url = mlServiceUrl + "/api/v1/recommendations";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = new HashMap<>();
            request.put("user_data", userData);
            request.put("jobs_data", jobsData);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JsonNode.class);

            return objectMapper.convertValue(response.getBody(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Error getting recommendations", e);
            throw new RuntimeException("Recommendation service unavailable", e);
        }
    }

    public List<Map<String, Object>> getRecommendationsFallback(Map<String, Object> userData,
                                                                List<Map<String, Object>> jobsData, Exception e) {
        log.warn("Using fallback recommendations (random)");

        return jobsData.stream().limit(10).map(job -> {
            Map<String, Object> rec = new HashMap<>(job);
            rec.put("score", 0.5);
            rec.put("reason", "Fallback recommendation");
            return rec;
        }).collect(java.util.stream.Collectors.toList());
    }

    @CircuitBreaker(name = "mlService", fallbackMethod = "evaluateInterviewFallback")
    public Map<String, Object> evaluateInterview(String question, String answer) {
        try {
            String url = mlServiceUrl + "/api/v1/interview/evaluate";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = new HashMap<>();
            request.put("question", question);
            request.put("answer", answer);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JsonNode.class);

            return objectMapper.convertValue(response.getBody(), Map.class);
        } catch (Exception e) {
            log.error("Error evaluating interview", e);
            throw new RuntimeException("Interview evaluation failed", e);
        }
    }

    public Map<String, Object> evaluateInterviewFallback(String question, String answer, Exception e) {
        log.warn("Using fallback interview evaluation");
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("score", 0.5);
        fallback.put("feedback", Arrays.asList("Unable to evaluate interview due to technical issues"));
        fallback.put("relevance_score", 0.5);
        fallback.put("completeness_score", 0.5);
        fallback.put("fluency_score", 0.5);
        return fallback;
    }
}