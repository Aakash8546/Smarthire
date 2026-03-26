package com.smarthire.service;

import com.smarthire.dto.MatchResponse;
import com.smarthire.model.*;
import com.smarthire.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private EmailService emailService;

    @Autowired
    private UserService userService;  // Add this

    @Transactional
    public MatchResponse matchResumeToJob(Long userId, Long jobId) {
        // Use UserService to get user
        User user = userService.getUserById(userId);  // Changed this line

        Resume resume = resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Resume not found. Please upload your resume first."));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        List<String> userSkills = resume.getSkillsList();
        List<String> jobSkills = job.getRequiredSkillsList();

        // Calculate match
        int matchPercentage = calculateMatchPercentage(userSkills, jobSkills);
        List<String> missingSkills = findMissingSkills(userSkills, jobSkills);
        List<String> matchingSkills = findMatchingSkills(userSkills, jobSkills);

        // Get AI recommendation
        String recommendation = aiService.getMatchingRecommendation(userSkills, jobSkills, matchPercentage);

        // Check if already applied
        boolean alreadyApplied = applicationRepository.existsByUserIdAndJobId(userId, jobId);

        // Save application if not already applied
        if (!alreadyApplied) {
            Application application = new Application();
            application.setUser(user);
            application.setJob(job);
            application.setMatchPercentage(matchPercentage);
            application.setMissingSkills(String.join(",", missingSkills));
            application.setMatchingSkills(String.join(",", matchingSkills));
            application.setStatus(ApplicationStatus.PENDING);
            applicationRepository.save(application);

            // Send email notification for high match
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

    // Remove this method since we're using UserService
    // public User getUserById(Long userId) {
    //     return userRepository.findById(userId)
    //             .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    // }

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