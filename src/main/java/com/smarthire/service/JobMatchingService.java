package com.smarthire.service;

import com.smarthire.dto.MatchResponse;
import com.smarthire.model.*;
import com.smarthire.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JobMatchingService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AIService aiService;

    @Autowired
    private AIGeminiService aiGeminiService;  // Add this

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    @Transactional
    public MatchResponse matchResumeToJob(Long userId, Long jobId) {
        User user = userService.getUserById(userId);

        Resume resume = resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Resume not found. Please upload your resume first."));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        List<String> userSkills = resume.getSkillsList();
        List<String> jobSkills = job.getRequiredSkillsList();

        int matchPercentage;
        List<String> missingSkills;
        List<String> matchingSkills;
        String recommendation;

        // Use AI for intelligent matching
        try {
            Map<String, Object> aiResult = aiGeminiService.calculateMatchWithAI(
                    userSkills, jobSkills, job.getTitle(), job.getDescription()
            );
            matchPercentage = (int) aiResult.getOrDefault("matchPercentage", 0);
            matchingSkills = (List<String>) aiResult.getOrDefault("matchingSkills", List.of());
            missingSkills = (List<String>) aiResult.getOrDefault("missingSkills", List.of());
            recommendation = (String) aiResult.getOrDefault("recommendation", "");
        } catch (Exception e) {
            // Fallback to keyword matching
            matchPercentage = calculateMatchPercentage(userSkills, jobSkills);
            missingSkills = findMissingSkills(userSkills, jobSkills);
            matchingSkills = findMatchingSkills(userSkills, jobSkills);
            recommendation = aiService.getMatchingRecommendation(userSkills, jobSkills, matchPercentage);
        }

        boolean alreadyApplied = applicationRepository.existsByUserIdAndJobId(userId, jobId);

        if (!alreadyApplied) {
            Application application = new Application();
            application.setUser(user);
            application.setJob(job);
            application.setMatchPercentage(matchPercentage);
            application.setMissingSkills(String.join(",", missingSkills));
            application.setMatchingSkills(String.join(",", matchingSkills));
            application.setStatus(ApplicationStatus.PENDING);
            applicationRepository.save(application);

            if (matchPercentage >= 70) {
                emailService.sendMatchNotification(user.getEmail(), job.getTitle(), matchPercentage, user.getName());
            }
        }

        MatchResponse response = new MatchResponse();
        response.setJobId(jobId);
        response.setJobTitle(job.getTitle());
        response.setCompany(job.getCompany());
        response.setMatchPercentage(matchPercentage);
        response.setMatchingSkills(matchingSkills);
        response.setMissingSkills(missingSkills);
        response.setRecommendation(recommendation);
        response.setAlreadyApplied(alreadyApplied);

        return response;
    }

    private int calculateMatchPercentage(List<String> userSkills, List<String> jobSkills) {
        if (jobSkills == null || jobSkills.isEmpty()) return 0;
        if (userSkills == null || userSkills.isEmpty()) return 0;

        long matchingSkills = userSkills.stream()
                .filter(skill -> jobSkills.stream().anyMatch(jobSkill ->
                        jobSkill.toLowerCase().contains(skill.toLowerCase()) ||
                                skill.toLowerCase().contains(jobSkill.toLowerCase())))
                .count();

        return (int) ((matchingSkills * 100) / jobSkills.size());
    }

    private List<String> findMissingSkills(List<String> userSkills, List<String> jobSkills) {
        if (jobSkills == null || jobSkills.isEmpty()) return List.of();
        if (userSkills == null || userSkills.isEmpty()) return jobSkills;

        return jobSkills.stream()
                .filter(jobSkill -> userSkills.stream().noneMatch(userSkill ->
                        userSkill.toLowerCase().contains(jobSkill.toLowerCase()) ||
                                jobSkill.toLowerCase().contains(userSkill.toLowerCase())))
                .collect(Collectors.toList());
    }

    private List<String> findMatchingSkills(List<String> userSkills, List<String> jobSkills) {
        if (userSkills == null || userSkills.isEmpty()) return List.of();
        if (jobSkills == null || jobSkills.isEmpty()) return List.of();

        return userSkills.stream()
                .filter(userSkill -> jobSkills.stream().anyMatch(jobSkill ->
                        jobSkill.toLowerCase().contains(userSkill.toLowerCase()) ||
                                userSkill.toLowerCase().contains(jobSkill.toLowerCase())))
                .collect(Collectors.toList());
    }
}