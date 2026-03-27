package com.smarthire.service;

import com.smarthire.dto.JobRecommendationResponse;
import com.smarthire.model.*;
import com.smarthire.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private SkillGapRoadmapRepository roadmapRepository;

    @Autowired
    private AIGeminiService aiGeminiService;

    @Autowired
    private UserService userService;

    @Transactional(readOnly = true)
    public List<JobRecommendationResponse> getPersonalizedRecommendations(Long userId, int limit) {
        Resume resume = resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Please upload your resume first"));

        List<String> userSkills = resume.getSkillsList();
        List<Job> allJobs = jobRepository.findActiveJobs();

        List<Job> eligibleJobs = allJobs.stream()
                .filter(job -> !applicationRepository.existsByUserIdAndJobId(userId, job.getId()))
                .collect(Collectors.toList());

        List<JobRecommendationResponse> recommendations = new ArrayList<>();

        try {
            List<Map<String, Object>> jobsForAI = new ArrayList<>();
            for (Job job : eligibleJobs) {
                Map<String, Object> jobMap = new HashMap<>();
                jobMap.put("id", job.getId());
                jobMap.put("title", job.getTitle());
                jobMap.put("skills", job.getRequiredSkillsList());
                jobsForAI.add(jobMap);
            }

            List<Map<String, Object>> aiRecommendations = aiGeminiService.getRecommendationsWithAI(userSkills, jobsForAI);

            for (Map<String, Object> aiRec : aiRecommendations) {
                Long jobId = ((Number) aiRec.get("id")).longValue();
                Job job = jobRepository.findById(jobId).orElse(null);
                if (job != null) {
                    JobRecommendationResponse dto = new JobRecommendationResponse();
                    dto.setJobId(job.getId());
                    dto.setTitle(job.getTitle());
                    dto.setCompany(job.getCompany());
                    dto.setLocation(job.getLocation());
                    dto.setMatchPercentage(((Number) aiRec.get("matchScore")).intValue());
                    dto.setRequiredSkills(job.getRequiredSkillsList());
                    dto.setSalaryRange(job.getSalaryRange());
                    dto.setMatchingSkills(findMatchingSkills(userSkills, job.getRequiredSkillsList()));
                    dto.setMissingSkills(findMissingSkills(userSkills, job.getRequiredSkillsList()));
                    recommendations.add(dto);
                }
            }
        } catch (Exception e) {
            for (Job job : eligibleJobs) {
                int matchScore = calculateMatchScore(userSkills, job.getRequiredSkillsList());
                if (matchScore >= 50) {
                    JobRecommendationResponse dto = new JobRecommendationResponse();
                    dto.setJobId(job.getId());
                    dto.setTitle(job.getTitle());
                    dto.setCompany(job.getCompany());
                    dto.setLocation(job.getLocation());
                    dto.setMatchPercentage(matchScore);
                    dto.setRequiredSkills(job.getRequiredSkillsList());
                    dto.setSalaryRange(job.getSalaryRange());
                    dto.setMatchingSkills(findMatchingSkills(userSkills, job.getRequiredSkillsList()));
                    dto.setMissingSkills(findMissingSkills(userSkills, job.getRequiredSkillsList()));
                    recommendations.add(dto);
                }
            }
            recommendations.sort((a, b) -> Integer.compare(b.getMatchPercentage(), a.getMatchPercentage()));
        }

        return recommendations.stream().limit(limit).collect(Collectors.toList());
    }

    // Method that returns SkillGapRoadmap (for controller)
    @Transactional
    public SkillGapRoadmap generateSkillGapRoadmap(Long userId, Long jobId) {
        Resume resume = resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        List<String> userSkills = resume.getSkillsList();
        List<String> jobSkills = job.getRequiredSkillsList();
        List<String> missingSkills = findMissingSkills(userSkills, jobSkills);

        SkillGapRoadmap roadmap = new SkillGapRoadmap();
        roadmap.setUserId(userId);
        roadmap.setJobId(jobId);

        String roadmapText;
        Integer estimatedWeeks;

        try {
            Map<String, Object> aiRoadmap = aiGeminiService.generateRoadmapWithAI(
                    userSkills, missingSkills, job.getTitle(), job.getDescription()
            );

            roadmapText = (String) aiRoadmap.getOrDefault("roadmap", "");
            estimatedWeeks = (Integer) aiRoadmap.getOrDefault("estimatedWeeks", missingSkills.size() * 2);

            if (roadmapText == null || roadmapText.isEmpty()) {
                roadmapText = generateFallbackRoadmap(missingSkills);
            }

        } catch (Exception e) {
            System.err.println("AI roadmap generation failed: " + e.getMessage());
            roadmapText = generateFallbackRoadmap(missingSkills);
            estimatedWeeks = missingSkills.isEmpty() ? 2 : missingSkills.size() * 2;
        }

        roadmap.setRoadmap(roadmapText);
        roadmap.setEstimatedWeeks(estimatedWeeks);
        roadmap.setCreatedAt(new Date());

        return roadmapRepository.save(roadmap);
    }

    // Method that returns Map (if needed elsewhere)
    @Transactional
    public Map<String, Object> generateSkillGapRoadmapWithDetails(Long userId, Long jobId) {
        Map<String, Object> response = new HashMap<>();

        try {
            SkillGapRoadmap roadmap = generateSkillGapRoadmap(userId, jobId);
            User user = userService.getUserById(userId);

            response.put("id", roadmap.getId());
            response.put("userId", userId);
            response.put("jobId", jobId);
            response.put("roadmap", roadmap.getRoadmap());
            response.put("estimatedWeeks", roadmap.getEstimatedWeeks());
            response.put("createdAt", roadmap.getCreatedAt());
            response.put("userName", user.getName());

        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", e.getMessage());
        }

        return response;
    }

    private int calculateMatchScore(List<String> userSkills, List<String> jobSkills) {
        if (jobSkills.isEmpty()) return 0;
        long matching = userSkills.stream()
                .filter(s -> jobSkills.stream().anyMatch(j -> j.toLowerCase().contains(s.toLowerCase())))
                .count();
        return (int) ((matching * 100) / jobSkills.size());
    }

    private List<String> findMatchingSkills(List<String> userSkills, List<String> jobSkills) {
        return userSkills.stream()
                .filter(s -> jobSkills.stream().anyMatch(j -> j.toLowerCase().contains(s.toLowerCase())))
                .collect(Collectors.toList());
    }

    private List<String> findMissingSkills(List<String> userSkills, List<String> jobSkills) {
        if (jobSkills == null || jobSkills.isEmpty()) return new ArrayList<>();
        if (userSkills == null || userSkills.isEmpty()) return jobSkills;

        return jobSkills.stream()
                .filter(s -> userSkills.stream().noneMatch(u -> u.toLowerCase().contains(s.toLowerCase())))
                .collect(Collectors.toList());
    }

    private String generateFallbackRoadmap(List<String> missingSkills) {
        if (missingSkills.isEmpty()) {
            return "✅ Your skills already match! You're ready to apply for this position.";
        }

        StringBuilder roadmap = new StringBuilder();
        roadmap.append("📚 **Learning Roadmap**\n\n");

        int week = 1;
        for (String skill : missingSkills) {
            roadmap.append(String.format("**Week %d-%d: Learn %s**\n", week, week + 1, skill));
            roadmap.append(String.format("• Study %s fundamentals\n", skill));
            roadmap.append(String.format("• Complete hands-on tutorials\n", skill));
            roadmap.append(String.format("• Build a small project using %s\n\n", skill));
            week += 2;
        }

        roadmap.append(String.format("**Week %d-%d: Practice Project**\n", week, week + 1));
        roadmap.append("• Build a comprehensive project combining all skills\n");
        roadmap.append("• Add to GitHub portfolio\n");
        roadmap.append("• Get code review from experienced developers\n\n");

        roadmap.append(String.format("**Week %d: Apply & Interview Prep**\n", week + 2));
        roadmap.append("• Update resume with new skills\n");
        roadmap.append("• Practice interview questions\n");
        roadmap.append("• Apply for the position\n");

        return roadmap.toString();
    }
}