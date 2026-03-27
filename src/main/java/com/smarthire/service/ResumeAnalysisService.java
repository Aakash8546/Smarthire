package com.smarthire.service;

import com.smarthire.dto.ResumeAnalysisResponse;
import com.smarthire.model.Resume;
import com.smarthire.model.User;
import com.smarthire.repository.ResumeRepository;
import com.smarthire.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ResumeAnalysisService {

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AIGeminiService aiGeminiService;

    @Autowired
    private ResumeTextExtractor resumeTextExtractor;

    // Complete fallback skill keywords with frontend skills
    private static final Set<String> FALLBACK_SKILL_KEYWORDS = new HashSet<>(Arrays.asList(
            // Frontend Skills
            "html", "html5", "css", "css3", "scss", "sass", "less", "tailwind", "bootstrap",
            "react", "react.js", "reactjs", "angular", "vue", "vue.js", "next.js", "nuxt.js",
            "javascript", "typescript", "jquery", "redux", "context api", "webpack", "vite",
            "figma", "adobe xd", "jest", "cypress", "react testing library", "material-ui",

            // Backend Skills
            "java", "python", "spring", "spring boot", "django", "flask", "node.js", "express",
            "mysql", "postgresql", "mongodb", "redis", "docker", "kubernetes", "aws", "azure", "gcp",
            "git", "jenkins", "rest api", "graphql", "microservices", "kafka", "c++", "c#", "php",
            "ruby", "swift", "kotlin", "go", "rust", "scala", "perl", "r", "matlab", "groovy",
            "spring cloud", "hibernate", "jpa", "oracle", "sql server", "cassandra", "firebase"
    ));

    public ResumeAnalysisResponse analyzeResume(Long userId, MultipartFile file) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String extractedText = resumeTextExtractor.extractText(file).toLowerCase();

            // Debug: Print extracted text length
            System.out.println("Extracted text length: " + extractedText.length());
            System.out.println("First 500 chars: " + (extractedText.length() > 500 ? extractedText.substring(0, 500) : extractedText));

            List<String> extractedSkills = new ArrayList<>();
            int aiScore = 0;
            List<String> skillSuggestions = new ArrayList<>();

            try {
                Map<String, Object> geminiResult = aiGeminiService.analyzeResumeWithAI(extractedText);
                extractedSkills = (List<String>) geminiResult.getOrDefault("skills", new ArrayList<>());
                aiScore = (int) geminiResult.getOrDefault("score", 50);
                skillSuggestions = (List<String>) geminiResult.getOrDefault("suggestions", new ArrayList<>());
                System.out.println("✅ AI resume analysis successful. Skills found: " + extractedSkills.size());
            } catch (Exception e) {
                System.err.println("❌ AI analysis failed: " + e.getMessage());
                System.err.println("Using fallback keyword matching...");
                extractedSkills = extractSkillsWithKeywords(extractedText);
                aiScore = calculateScore(extractedSkills);
                skillSuggestions = generateSuggestions(extractedSkills);
                System.out.println("Fallback skills found: " + extractedSkills);
            }

            Resume resume = new Resume();
            resume.setUser(user);
            resume.setFileName(file.getOriginalFilename());
            resume.setContent(extractedText);
            resume.setExtractedSkills(String.join(",", extractedSkills));
            resume.setAiScore(aiScore);
            resume.setSkillSuggestions(String.join(",", skillSuggestions));
            resume.setAnalysisDate(LocalDateTime.now());

            Optional<Resume> existingResume = resumeRepository.findByUser(user);
            if (existingResume.isPresent()) {
                resume.setId(existingResume.get().getId());
            }

            resumeRepository.save(resume);

            ResumeAnalysisResponse response = new ResumeAnalysisResponse();
            response.setUserId(userId);
            response.setFileName(file.getOriginalFilename());
            response.setExtractedSkills(extractedSkills);
            response.setScore(aiScore);
            response.setSuggestions(skillSuggestions);
            response.setAnalysisDate(LocalDateTime.now());

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to analyze resume: " + e.getMessage());
        }
    }

    private List<String> extractSkillsWithKeywords(String text) {
        Set<String> skills = new LinkedHashSet<>();

        for (String keyword : FALLBACK_SKILL_KEYWORDS) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(text).find()) {
                skills.add(keyword);
            }
        }

        return new ArrayList<>(skills);
    }

    private int calculateScore(List<String> skills) {
        if (skills.isEmpty()) return 0;
        if (skills.size() <= 3) return 40;
        if (skills.size() <= 5) return 55;
        if (skills.size() <= 8) return 70;
        if (skills.size() <= 12) return 85;
        return 95;
    }

    private List<String> generateSuggestions(List<String> currentSkills) {
        List<String> suggestions = new ArrayList<>();
        Set<String> lowerSkills = new HashSet<>();
        for (String s : currentSkills) {
            lowerSkills.add(s.toLowerCase());
        }

        List<String> trendingSkills = Arrays.asList(
                "Docker", "Kubernetes", "AWS", "Microservices",
                "React", "TypeScript", "GraphQL", "Next.js", "Tailwind CSS"
        );

        for (String skill : trendingSkills) {
            if (!lowerSkills.contains(skill.toLowerCase()) && suggestions.size() < 5) {
                suggestions.add(skill);
            }
        }

        return suggestions;
    }

    public ResumeAnalysisResponse getResumeAnalysis(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resume resume = resumeRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        ResumeAnalysisResponse response = new ResumeAnalysisResponse();
        response.setUserId(userId);
        response.setFileName(resume.getFileName());
        response.setExtractedSkills(resume.getSkillsList());
        response.setScore(resume.getAiScore());
        response.setSuggestions(resume.getSkillSuggestionsList());
        response.setAnalysisDate(resume.getAnalysisDate());

        return response;
    }

    public boolean hasResume(Long userId) {
        return resumeRepository.existsByUserId(userId);
    }
}