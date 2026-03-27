package com.smarthire.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIGeminiService {

    @Value("${ai.service.api-key}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent}")
    private String geminiApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> analyzeResumeWithAI(String resumeText) {
        Map<String, Object> result = new HashMap<>();

        try {
            String truncatedText = resumeText.length() > 20000 ? resumeText.substring(0, 20000) : resumeText;

            String prompt = """
                You are an expert resume analyzer. Extract ALL technical skills from this resume.
                
                Resume:
                """ + truncatedText + """
                
                Return ONLY a valid JSON object. No markdown, no extra text.
                {
                    "skills": ["skill1", "skill2", "skill3"],
                    "experienceYears": number,
                    "education": "degree, university",
                    "score": number between 0-100,
                    "suggestions": ["improvement1", "improvement2"]
                }
                
                Rules:
                - skills: List every technical skill, programming language, framework, tool
                - score: Based on number and quality of skills
                - suggestions: Provide 2-4 specific, actionable improvements
                """;

            String response = callGemini(prompt);
            response = response.replace("```json", "").replace("```", "").trim();
            result = parseJsonResponse(response);

            if (result.get("skills") == null) result.put("skills", new ArrayList<>());
            if (result.get("score") == null) result.put("score", 50);
            if (result.get("suggestions") == null) result.put("suggestions", Arrays.asList("Add more skills", "Improve resume format"));

        } catch (Exception e) {
            System.err.println("AI resume analysis failed: " + e.getMessage());
            result.put("skills", new ArrayList<>());
            result.put("score", 50);
            result.put("suggestions", Arrays.asList("Upload a more detailed resume", "Add technical skills"));
        }

        return result;
    }

    public Map<String, Object> calculateMatchWithAI(List<String> userSkills, List<String> jobSkills,
                                                    String jobTitle, String jobDescription) {
        Map<String, Object> result = new HashMap<>();

        try {
            String prompt = """
                You are an AI hiring assistant. Analyze the match between candidate skills and job requirements.
                
                Candidate Skills: %s
                Job Title: %s
                Job Skills: %s
                Job Description: %s
                
                Return ONLY a valid JSON object:
                {
                    "matchPercentage": number between 0-100,
                    "matchingSkills": ["skill1", "skill2"],
                    "missingSkills": ["skill1", "skill2"],
                    "recommendation": "short recommendation"
                }
                """.formatted(
                    String.join(", ", userSkills),
                    jobTitle,
                    String.join(", ", jobSkills),
                    jobDescription != null ? jobDescription.substring(0, Math.min(500, jobDescription.length())) : ""
            );

            String response = callGemini(prompt);
            response = response.replace("```json", "").replace("```", "").trim();
            result = parseJsonResponse(response);

        } catch (Exception e) {
            System.err.println("AI match calculation failed: " + e.getMessage());
            result.put("matchPercentage", calculateFallbackMatch(userSkills, jobSkills));
            result.put("matchingSkills", findMatchingSkills(userSkills, jobSkills));
            result.put("missingSkills", findMissingSkills(userSkills, jobSkills));
            result.put("recommendation", "Consider learning: " + String.join(", ", findMissingSkills(userSkills, jobSkills)));
        }

        return result;
    }

    public List<Map<String, Object>> getRecommendationsWithAI(List<String> userSkills, List<Map<String, Object>> jobs) {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        try {
            List<Map<String, Object>> topJobs = jobs.size() > 10 ? jobs.subList(0, 10) : jobs;
            StringBuilder jobsStr = new StringBuilder();

            for (Map<String, Object> job : topJobs) {
                jobsStr.append("- Job ID ").append(job.get("id"))
                        .append(": ").append(job.get("title"))
                        .append(" (Skills: ").append(job.get("skills")).append(")\n");
            }

            String prompt = """
                You are an AI career advisor. Recommend top 5 jobs for this candidate.
                
                Candidate Skills: %s
                
                Available Jobs:
                %s
                
                Return ONLY a JSON array:
                [
                    {"jobId": 1, "matchScore": 85, "reason": "Excellent skill match"}
                ]
                """.formatted(String.join(", ", userSkills), jobsStr.toString());

            String response = callGemini(prompt);
            response = response.replace("```json", "").replace("```", "").trim();
            List<Map<String, Object>> aiRecommendations = parseJsonArrayResponse(response);

            for (Map<String, Object> job : jobs) {
                for (Map<String, Object> rec : aiRecommendations) {
                    if (rec.get("jobId").toString().equals(job.get("id").toString())) {
                        Map<String, Object> enriched = new HashMap<>(job);
                        enriched.put("matchScore", rec.get("matchScore"));
                        enriched.put("matchReason", rec.get("reason"));
                        recommendations.add(enriched);
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("AI recommendations failed: " + e.getMessage());
            recommendations = getFallbackRecommendations(userSkills, jobs);
        }

        return recommendations;
    }

    public Map<String, Object> generateRoadmapWithAI(List<String> userSkills, List<String> missingSkills,
                                                     String jobTitle, String jobDescription) {
        Map<String, Object> result = new HashMap<>();

        try {
            String missingSkillsStr = missingSkills.isEmpty() ? "None (skills already match)" : String.join(", ", missingSkills);

            // Simplified, more direct prompt
            String prompt = """
                Create a detailed learning roadmap for these skills.
                
                Skills to learn: %s
                Target job: %s
                
                Return ONLY valid JSON:
                {
                    "roadmap": "Week 1-2: Learn SkillName\\n  • Action item 1\\n  • Action item 2\\n\\nWeek 3-4: Learn NextSkill\\n  • Action item 1\\n  • Action item 2",
                    "estimatedWeeks": number,
                    "resources": ["resource1", "resource2"],
                    "projects": ["project1", "project2"]
                }
                """.formatted(missingSkillsStr, jobTitle);

            String response = callGemini(prompt);
            System.out.println("Gemini roadmap response length: " + response.length());

            response = response.replace("```json", "").replace("```", "").trim();
            result = parseJsonResponse(response);

            // Check if roadmap is valid
            String roadmap = (String) result.get("roadmap");
            if (roadmap == null || roadmap.isEmpty() || roadmap.contains("Week 1-2: Learn")) {
                System.out.println("Using enhanced fallback roadmap");
                result.put("roadmap", generateEnhancedRoadmap(missingSkills, jobTitle));
                result.put("estimatedWeeks", missingSkills.isEmpty() ? 2 : missingSkills.size() * 2);
                result.put("resources", generateResources(missingSkills));
                result.put("projects", generateProjects(missingSkills, jobTitle));
            }

        } catch (Exception e) {
            System.err.println("AI roadmap generation failed: " + e.getMessage());
            result.put("roadmap", generateEnhancedRoadmap(missingSkills, jobTitle));
            result.put("estimatedWeeks", missingSkills.isEmpty() ? 2 : missingSkills.size() * 2);
            result.put("resources", generateResources(missingSkills));
            result.put("projects", generateProjects(missingSkills, jobTitle));
        }

        return result;
    }

    private String callGemini(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", new Object[]{part});
        requestBody.put("contents", new Object[]{content});

        String url = geminiApiUrl + "?key=" + geminiApiKey;
        System.out.println("Calling Gemini API...");

        Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

        if (response != null && response.containsKey("candidates")) {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (!candidates.isEmpty()) {
                Map<String, Object> candidate = candidates.get(0);
                Map<String, Object> contentObj = (Map<String, Object>) candidate.get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) contentObj.get("parts");
                String text = (String) parts.get(0).get("text");
                return text.replace("```json", "").replace("```", "").trim();
            }
        }
        throw new RuntimeException("No response from Gemini");
    }

    private Map<String, Object> parseJsonResponse(String responseText) {
        Map<String, Object> result = new HashMap<>();
        try {
            JsonNode node = objectMapper.readTree(responseText);

            if (node.has("skills") && node.get("skills").isArray()) {
                List<String> skills = new ArrayList<>();
                node.get("skills").forEach(s -> skills.add(s.asText()));
                result.put("skills", skills);
            }
            if (node.has("score")) result.put("score", node.get("score").asInt());
            if (node.has("suggestions") && node.get("suggestions").isArray()) {
                List<String> suggestions = new ArrayList<>();
                node.get("suggestions").forEach(s -> suggestions.add(s.asText()));
                result.put("suggestions", suggestions);
            }
            if (node.has("matchPercentage")) result.put("matchPercentage", node.get("matchPercentage").asInt());
            if (node.has("matchingSkills") && node.get("matchingSkills").isArray()) {
                List<String> skills = new ArrayList<>();
                node.get("matchingSkills").forEach(s -> skills.add(s.asText()));
                result.put("matchingSkills", skills);
            }
            if (node.has("missingSkills") && node.get("missingSkills").isArray()) {
                List<String> skills = new ArrayList<>();
                node.get("missingSkills").forEach(s -> skills.add(s.asText()));
                result.put("missingSkills", skills);
            }
            if (node.has("recommendation")) result.put("recommendation", node.get("recommendation").asText());
            if (node.has("roadmap")) result.put("roadmap", node.get("roadmap").asText());
            if (node.has("estimatedWeeks")) result.put("estimatedWeeks", node.get("estimatedWeeks").asInt());
            if (node.has("resources") && node.get("resources").isArray()) {
                List<String> resources = new ArrayList<>();
                node.get("resources").forEach(r -> resources.add(r.asText()));
                result.put("resources", resources);
            }
            if (node.has("projects") && node.get("projects").isArray()) {
                List<String> projects = new ArrayList<>();
                node.get("projects").forEach(p -> projects.add(p.asText()));
                result.put("projects", projects);
            }

        } catch (Exception e) {
            System.err.println("Failed to parse JSON: " + e.getMessage());
            result.put("skills", new ArrayList<>());
            result.put("score", 50);
        }
        return result;
    }

    private List<Map<String, Object>> parseJsonArrayResponse(String responseText) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            JsonNode node = objectMapper.readTree(responseText);
            if (node.isArray()) {
                for (JsonNode item : node) {
                    Map<String, Object> map = new HashMap<>();
                    if (item.has("jobId")) map.put("jobId", item.get("jobId").asInt());
                    if (item.has("matchScore")) map.put("matchScore", item.get("matchScore").asInt());
                    if (item.has("reason")) map.put("reason", item.get("reason").asText());
                    result.add(map);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON array: " + e.getMessage());
        }
        return result;
    }

    private int calculateFallbackMatch(List<String> userSkills, List<String> jobSkills) {
        if (jobSkills.isEmpty()) return 0;
        long matching = userSkills.stream()
                .filter(s -> jobSkills.stream().anyMatch(j -> j.toLowerCase().contains(s.toLowerCase())))
                .count();
        return (int) ((matching * 100) / jobSkills.size());
    }

    private List<String> findMatchingSkills(List<String> userSkills, List<String> jobSkills) {
        return userSkills.stream()
                .filter(s -> jobSkills.stream().anyMatch(j -> j.toLowerCase().contains(s.toLowerCase())))
                .toList();
    }

    private List<String> findMissingSkills(List<String> userSkills, List<String> jobSkills) {
        if (jobSkills == null || jobSkills.isEmpty()) return new ArrayList<>();
        return jobSkills.stream()
                .filter(s -> userSkills.stream().noneMatch(u -> u.toLowerCase().contains(s.toLowerCase())))
                .toList();
    }

    private List<Map<String, Object>> getFallbackRecommendations(List<String> userSkills, List<Map<String, Object>> jobs) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        for (Map<String, Object> job : jobs) {
            List<String> jobSkills = (List<String>) job.get("skills");
            int match = calculateFallbackMatch(userSkills, jobSkills);
            if (match >= 50) {
                Map<String, Object> enriched = new HashMap<>(job);
                enriched.put("matchScore", match);
                recommendations.add(enriched);
            }
        }
        recommendations.sort((a, b) -> Integer.compare((int) b.get("matchScore"), (int) a.get("matchScore")));
        return recommendations;
    }

    private String generateEnhancedRoadmap(List<String> missingSkills, String jobTitle) {
        if (missingSkills.isEmpty()) {
            return "✅ Your skills already match! You're ready to apply for " + jobTitle;
        }

        StringBuilder roadmap = new StringBuilder();
        int week = 1;
        for (String skill : missingSkills) {
            roadmap.append(String.format("**Week %d-%d: Master %s**\n", week, week + 1, skill));
            roadmap.append(String.format("  • Learn %s fundamentals and best practices\n", skill));
            roadmap.append(String.format("  • Complete hands-on tutorials and exercises\n", skill));
            roadmap.append(String.format("  • Build a mini-project using %s\n", skill));
            roadmap.append("\n");
            week += 2;
        }

        roadmap.append(String.format("**Week %d-%d: Integration Project**\n", week, week + 1));
        roadmap.append("  • Build a complete project combining all skills\n");
        roadmap.append("  • Deploy and test thoroughly\n");
        roadmap.append("  • Add to portfolio with documentation\n");
        roadmap.append("\n");

        roadmap.append(String.format("**Week %d: Final Preparation**\n", week + 2));
        roadmap.append("  • Update resume with new skills\n");
        roadmap.append("  • Practice interview questions\n");
        roadmap.append("  • Apply for " + jobTitle + " positions\n");

        return roadmap.toString();
    }

    private List<String> generateResources(List<String> missingSkills) {
        List<String> resources = new ArrayList<>();
        Map<String, List<String>> skillResources = new HashMap<>();

        skillResources.put("react", Arrays.asList("React Official Docs - react.dev", "React Course - freeCodeCamp", "The Road to React - Robin Wieruch"));
        skillResources.put("spring", Arrays.asList("Spring Boot Documentation - spring.io", "Spring Boot Course - Baeldung", "Spring in Action - Craig Walls"));
        skillResources.put("docker", Arrays.asList("Docker Documentation - docs.docker.com", "Docker Course - KodeKloud", "Docker Deep Dive - Nigel Poulton"));
        skillResources.put("aws", Arrays.asList("AWS Free Tier - aws.amazon.com", "AWS Certified Developer Course - Udemy", "AWS Documentation"));
        skillResources.put("javascript", Arrays.asList("JavaScript.info", "You Don't Know JS - Kyle Simpson", "MDN Web Docs"));
        skillResources.put("python", Arrays.asList("Python.org - Official Docs", "Automate the Boring Stuff", "Real Python"));
        skillResources.put("typescript", Arrays.asList("TypeScript Handbook", "TypeScript Deep Dive", "Total TypeScript Course"));

        for (String skill : missingSkills) {
            String lowerSkill = skill.toLowerCase();
            boolean found = false;
            for (Map.Entry<String, List<String>> entry : skillResources.entrySet()) {
                if (lowerSkill.contains(entry.getKey())) {
                    resources.addAll(entry.getValue());
                    found = true;
                    break;
                }
            }
            if (!found) {
                resources.add(skill + " Official Documentation");
                resources.add(skill + " Course - Coursera/Udemy");
            }
        }

        return resources.stream().distinct().limit(5).collect(Collectors.toList());
    }

    private List<String> generateProjects(List<String> missingSkills, String jobTitle) {
        List<String> projects = new ArrayList<>();
        String mainSkill = missingSkills.isEmpty() ? "new skills" : missingSkills.get(0);

        if (jobTitle.toLowerCase().contains("frontend") || jobTitle.toLowerCase().contains("react")) {
            projects.add("Build a modern web application using " + String.join(", ", missingSkills));
            projects.add("Create a responsive portfolio website showcasing your projects");
            projects.add("Build a real-time dashboard with charts and data visualization");
        } else if (jobTitle.toLowerCase().contains("backend") || jobTitle.toLowerCase().contains("spring")) {
            projects.add("Build a RESTful API with " + String.join(", ", missingSkills));
            projects.add("Create a microservices-based e-commerce backend");
            projects.add("Build a real-time chat application with WebSockets");
        } else if (jobTitle.toLowerCase().contains("full stack")) {
            projects.add("Build a full-stack application with " + String.join(", ", missingSkills));
            projects.add("Create a social media clone with authentication and CRUD operations");
            projects.add("Build a task management system with real-time updates");
        } else {
            projects.add("Build a real-world project using " + mainSkill);
            projects.add("Create a GitHub repository with well-documented code");
            projects.add("Contribute to open-source projects in " + mainSkill);
        }

        return projects.stream().distinct().limit(3).collect(Collectors.toList());
    }
}