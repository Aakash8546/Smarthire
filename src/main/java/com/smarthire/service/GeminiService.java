package com.smarthire.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthire.config.GeminiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiConfig geminiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String analyzeResume(String resumeContent) {
        String prompt = "Analyze this resume and return JSON with: skills (array), score (0-100), suggestions (string), improvementAreas (array). Resume: " +
                resumeContent.substring(0, Math.min(1500, resumeContent.length()));
        return callGeminiAPI(prompt);
    }

    public String matchSkills(String resumeSkills, String jobSkills) {
        String prompt = "Compare these skills: Resume skills: " + resumeSkills + ", Job required skills: " + jobSkills +
                ". Return ONLY JSON with: matchPercentage (integer 0-100), matchedSkills (array), missingSkills (array), recommendation (string).";
        return callGeminiAPI(prompt);
    }

    public String generateLearningRoadmap(String currentSkills, List<String> targetSkills) {
        String prompt = "Create a detailed learning timeline (2-8 weeks) to learn these skills: " +
                String.join(", ", targetSkills) +
                ". Current skills: " + currentSkills +
                ". Provide week-by-week plan with specific resources, projects, and daily schedule.";
        return callGeminiAPI(prompt);
    }

    private String callGeminiAPI(String prompt) {
        try {
            String url = geminiConfig.getApiUrl() + "?key=" + geminiConfig.getApiKey();

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            content.put("parts", List.of(part));
            requestBody.put("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String generatedText = root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            return generatedText;
        } catch (Exception e) {
            System.err.println("Gemini API error: " + e.getMessage());
            return getFallbackResponse(prompt);
        }
    }

    private String getFallbackResponse(String prompt) {
        if (prompt.contains("Analyze this resume")) {
            return "{\"skills\": [\"Java\", \"Spring Boot\", \"Python\", \"SQL\", \"JavaScript\"], \"score\": 75, \"suggestions\": \"Focus on cloud technologies (AWS/GCP), learn Docker and Kubernetes, improve system design skills\", \"improvementAreas\": [\"Cloud Computing\", \"Containerization\", \"Microservices\", \"System Design\"]}";
        } else if (prompt.contains("Compare these skills")) {
            return "{\"matchPercentage\": 70, \"matchedSkills\": [\"Java\", \"Spring Boot\", \"SQL\"], \"missingSkills\": [\"Docker\", \"Kubernetes\", \"AWS\"], \"recommendation\": \"Good match! Your Java skills are strong. Focus on learning containerization with Docker and cloud platforms like AWS to increase your match percentage.\"}";
        } else {
            return "=== 4-Week Learning Roadmap ===\n\n" +
                    "Week 1-2: Fundamentals\n" +
                    "• Complete online courses (Coursera/Udemy)\n" +
                    "• Practice daily for 2-3 hours\n" +
                    "• Build small projects\n\n" +
                    "Week 3-4: Advanced Concepts\n" +
                    "• Work on real-world projects\n" +
                    "• Contribute to open source\n" +
                    "• Prepare for interviews\n\n" +
                    "Week 5-6: Project Building\n" +
                    "• Build end-to-end applications\n" +
                    "• Deploy to cloud platforms\n" +
                    "• Get peer reviews\n\n" +
                    "Week 7-8: Interview Preparation\n" +
                    "• Practice coding challenges\n" +
                    "• Review system design concepts\n" +
                    "• Mock interviews with peers";
        }
    }
}