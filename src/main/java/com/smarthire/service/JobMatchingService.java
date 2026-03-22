package com.smarthire.service;

import com.smarthire.dto.MatchResponse;
import com.smarthire.model.*;
import com.smarthire.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private AIService aiService;

    @Autowired
    private EmailService emailService;

    public MatchResponse matchResumeToJob(Long userId, Long jobId) {
        User user = new User();
        user.setId(userId);

        Resume resume = resumeRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Resume not found. Please upload your resume first."));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        List<String> userSkills = resume.getSkillsList();
        List<String> jobSkills = job.getRequiredSkillsList();

        // Calculate match percentage
        int matchPercentage = calculateMatchPercentage(userSkills, jobSkills);
        List<String> missingSkills = findMissingSkills(userSkills, jobSkills);
        List<String> matchingSkills = findMatchingSkills(userSkills, jobSkills);

        // Get AI recommendation
        String recommendation = aiService.getMatchingRecommendation(userSkills, jobSkills, matchPercentage);

        // Check if already applied
        boolean alreadyApplied = applicationRepository.existsByUserAndJob(user, job);

        // Save application if not exists
        if (!alreadyApplied) {
            Application application = new Application();
            application.setUser(user);
            application.setJob(job);
            application.setMatchPercentage(matchPercentage);
            application.setMissingSkills(String.join(",", missingSkills));
            application.setMatchingSkills(String.join(",", matchingSkills));
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

    private int calculateMatchPercentage(List<String> userSkills, List<String> jobSkills) {
        if (jobSkills.isEmpty()) return 0;

        long matchingSkills = userSkills.stream()
                .filter(skill -> jobSkills.stream().anyMatch(jobSkill ->
                        jobSkill.toLowerCase().contains(skill.toLowerCase()) ||
                                skill.toLowerCase().contains(jobSkill.toLowerCase())))
                .count();

        return (int) ((matchingSkills * 100) / jobSkills.size());
    }

    private List<String> findMissingSkills(List<String> userSkills, List<String> jobSkills) {
        return jobSkills.stream()
                .filter(jobSkill -> userSkills.stream().noneMatch(userSkill ->
                        userSkill.toLowerCase().contains(jobSkill.toLowerCase()) ||
                                jobSkill.toLowerCase().contains(userSkill.toLowerCase())))
                .collect(Collectors.toList());
    }

    private List<String> findMatchingSkills(List<String> userSkills, List<String> jobSkills) {
        return userSkills.stream()
                .filter(userSkill -> jobSkills.stream().anyMatch(jobSkill ->
                        jobSkill.toLowerCase().contains(userSkill.toLowerCase()) ||
                                userSkill.toLowerCase().contains(jobSkill.toLowerCase())))
                .collect(Collectors.toList());
    }
}