package com.smarthire.service;

import com.smarthire.dto.ResumeAnalysisResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AIService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ai.service.api-key}")
    private String apiKey;

    @Value("${ai.service.url}")
    private String apiUrl;

    @Value("${ai.service.provider}")
    private String provider;

    public ResumeAnalysisResponse analyzeResume(String resumeText) {

        String truncatedText = resumeText.length() > 10000 ? resumeText.substring(0, 10000) : resumeText;

        String prompt = "You are an expert resume analyzer. Analyze this resume and provide response in the following exact format:\n" +
                "SKILLS: [comma-separated list of technical and soft skills found]\n" +
                "SCORE: [number between 0-100 based on resume quality, structure, and completeness]\n" +
                "SUGGESTIONS: [bullet points with specific improvement suggestions, each on new line starting with *]\n\n" +
                "Resume content:\n" + truncatedText;

        String aiResponse = callAIService(prompt);

        ResumeAnalysisResponse response = new ResumeAnalysisResponse();
        List<String> skills = extractSkills(aiResponse);
        response.setExtractedSkills(skills.isEmpty() ? getDefaultSkills() : skills);
        response.setScore(extractScore(aiResponse));
        response.setSuggestions(extractSuggestions(aiResponse));

        return response;
    }

    public String getMatchingRecommendation(List<String> userSkills, List<String> jobSkills, int matchPercentage) {
        String prompt = String.format(
                "You are a career advisor. Given:\n" +
                        "Candidate skills: %s\n" +
                        "Job requirements: %s\n" +
                        "Current match: %d%%\n\n" +
                        "Provide a brief, encouraging recommendation (2-3 sentences) on how to improve this match.",
                String.join(", ", userSkills),
                String.join(", ", jobSkills),
                matchPercentage
        );

        return callAIService(prompt);
    }


        String response = callAIService(prompt);
        try {

            String numberStr = response.trim().replaceAll("[^0-9]", "");
            if (!numberStr.isEmpty()) {
                int score = Integer.parseInt(numberStr);
                return Math.min(100, Math.max(0, score));
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing score: " + e.getMessage());
        }
        return 50;
    }

    public String generateSkillGapRoadmap(List<String> missingSkills, String jobTitle) {
        if (missingSkills.isEmpty()) {
            return "Congratulations! You already have all the required skills for this position.";
        }

        String prompt = String.format(
                "Create a concise learning roadmap to acquire these skills for a %s position within 2-8 weeks:\n%s\n\n" +
                        "Provide week-by-week plan with specific learning resources, courses, and practical milestones.\n" +
                        "Format as:\n" +
                        "Week 1-2: [activities]\n" +
                        "Week 3-4: [activities]\n" +
                        "Week 5-6: [activities]\n" +
                        "Week 7-8: [activities]",
                jobTitle,
                String.join(", ", missingSkills)
        );

        return callAIService(prompt);
    }

    private String callAIService(String prompt) {
        try {
            if ("gemini".equalsIgnoreCase(provider)) {
                return callGeminiAPI(prompt);
            } else if ("openai".equalsIgnoreCase(provider)) {
                return callOpenAIAPI(prompt);
            } else {
                return getFallbackResponse(prompt);
            }
        } catch (Exception e) {
            System.err.println("AI Service error: " + e.getMessage());
            e.printStackTrace();
            return getFallbackResponse(prompt);
        }
    }

    private String callGeminiAPI(String prompt) {
        try {

            String url = apiUrl + "?key=" + apiKey;


            Map<String, Object> requestBody = new HashMap<>();


            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();


            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            parts.add(part);

            content.put("parts", parts);
            contents.add(content);
            requestBody.put("contents", contents);


            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 1024);
            requestBody.put("generationConfig", generationConfig);


            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);


            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );


            if (response.getBody() != null && response.getBody().containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    Map<String, Object> contentResponse = (Map<String, Object>) candidate.get("content");
                    List<Map<String, Object>> responseParts = (List<Map<String, Object>>) contentResponse.get("parts");
                    if (!responseParts.isEmpty()) {
                        String text = (String) responseParts.get(0).get("text");
                        System.out.println("Gemini API response received successfully");
                        return text;
                    }
                }
            }

            System.err.println("Unexpected Gemini API response format");
            return getFallbackResponse(prompt);

        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            e.printStackTrace();
            return getFallbackResponse(prompt);
        }
    }

    private String callOpenAIAPI(String prompt) {
        try {

            String url = "https://api.openai.com/v1/chat/completions";


            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a helpful career advisor and resume analyzer.");
            messages.add(systemMessage);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 1000);


            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);


            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );


            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) choice.get("message");
                    return message.get("content");
                }
            }

            return getFallbackResponse(prompt);

        } catch (Exception e) {
            System.err.println("Error calling OpenAI API: " + e.getMessage());
            return getFallbackResponse(prompt);
        }
    }

    private String getFallbackResponse(String prompt) {
        System.out.println("Using fallback response for prompt: " + prompt.substring(0, Math.min(100, prompt.length())));

        if (prompt.contains("expert resume analyzer")) {
            return "SKILLS: Java, Python, Spring Boot, React, SQL, AWS, Docker, Git, REST APIs, Microservices\n" +
                    "SCORE: 78\n" +
                    "SUGGESTIONS:\n" +
                    "* Add more quantifiable achievements with metrics (e.g., 'Improved performance by 30%')\n" +
                    "* Include relevant certifications like AWS Certified Developer or Oracle Java Certification\n" +
                    "* Improve project descriptions with specific technologies used and your role\n" +
                    "* Add soft skills like team collaboration, leadership, and communication\n" +
                    "* Include links to GitHub portfolio or LinkedIn profile";
        } else if (prompt.contains("learning roadmap")) {
            return "Week 1-2: Complete online courses\n" +
                    "- Udemy: 'Complete Java Masterclass' (10 hours)\n" +
                    "- Coursera: 'Spring Boot Fundamentals' (8 hours)\n" +
                    "- Build a simple CRUD application\n\n" +
                    "Week 3-4: Build portfolio projects\n" +
                    "- Create a REST API with Spring Boot\n" +
                    "- Integrate with PostgreSQL database\n" +
                    "- Add JWT authentication\n\n" +
                    "Week 5-6: Advanced topics\n" +
                    "- Learn Docker and containerization\n" +
                    "- Study microservices architecture\n" +
                    "- Contribute to open source projects\n\n" +
                    "Week 7-8: Interview preparation\n" +
                    "- Practice coding challenges on LeetCode\n" +
                    "- Prepare system design questions\n" +
                    "- Update resume with new skills and projects";
        } else if (prompt.contains("suitability")) {
            return "75";
        } else if (prompt.contains("career advisor")) {
            return "Based on your skills profile, you have a strong foundation. To improve your match percentage, focus on gaining practical experience with the missing technologies through hands-on projects. Consider obtaining relevant certifications to validate your expertise. The job market values demonstrated skills through portfolio projects as much as formal experience.";
        } else {
            return "Based on your skills profile, focus on strengthening your core technical competencies and building practical projects to demonstrate your abilities. Consider online certifications to validate your expertise. The key is to show practical application of your skills through concrete examples.";
        }
    }

    private List<String> extractSkills(String response) {
        List<String> skills = new ArrayList<>();
        if (response.contains("SKILLS:")) {
            try {
                String skillsPart = response.split("SKILLS:")[1].split("\n")[0];
                for (String skill : skillsPart.split(",")) {
                    String trimmed = skill.trim();
                    if (!trimmed.isEmpty()) {
                        skills.add(trimmed);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error extracting skills: " + e.getMessage());
            }
        }
        return skills;
    }

    private int extractScore(String response) {
        if (response.contains("SCORE:")) {
            try {
                String scorePart = response.split("SCORE:")[1].split("\n")[0];
                return Integer.parseInt(scorePart.trim());
            } catch (Exception e) {
                System.err.println("Error extracting score: " + e.getMessage());
            }
        }
        return 70;
    }

    private List<String> extractSuggestions(String response) {
        List<String> suggestions = new ArrayList<>();
        if (response.contains("SUGGESTIONS:")) {
            try {
                String suggestionsPart = response.split("SUGGESTIONS:")[1];
                String[] lines = suggestionsPart.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("*") || line.startsWith("-")) {
                        suggestions.add(line.substring(1).trim());
                    } else if (!line.isEmpty() && !line.contains("SKILLS:") && !line.contains("SCORE:")) {
                        suggestions.add(line);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error extracting suggestions: " + e.getMessage());
            }
        }
        return suggestions.isEmpty() ? Arrays.asList(
                "Add more quantifiable achievements",
                "Include relevant certifications",
                "Improve project descriptions",
                "Add soft skills like communication and teamwork"
        ) : suggestions;
    }

    private List<String> getDefaultSkills() {
        return Arrays.asList("Java", "Spring Boot", "SQL", "REST APIs", "Git", "Problem Solving");
    }
}