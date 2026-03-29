
package com.smarthire.service;

import com.smarthire.entity.*;
import com.smarthire.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecommendationService {
    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private MLServiceClient mlServiceClient;

    @Autowired
    private ResumeRepository resumeRepository;

    @Cacheable(value = "recommendations", key = "#userId")
    public List<Job> getRecommendations(Long userId, int limit) {
        log.info("Getting recommendations for user: {}", userId);

        Resume resume = resumeRepository.findByUserId(userId).orElse(null);
        if (resume == null || resume.getParsedText() == null) {
            // Cold start: return recent jobs
            return jobRepository.findTopNByOrderByCreatedAtDesc(limit);
        }

        // Get all active jobs
        List<Job> activeJobs = jobRepository.findByStatus("ACTIVE");

        // Filter out already applied jobs
        Set<Long> appliedJobIds = applicationRepository.findByUserId(userId)
                .stream()
                .map(app -> app.getJob().getId())
                .collect(Collectors.toSet());

        List<Job> eligibleJobs = activeJobs.stream()
                .filter(job -> !appliedJobIds.contains(job.getId()))
                .collect(Collectors.toList());

        if (eligibleJobs.isEmpty()) {
            return Collections.emptyList();
        }

        // Prepare data for ML service
        List<Map<String, Object>> jobsData = new ArrayList<>();
        for (Job job : eligibleJobs) {
            Map<String, Object> jobData = new HashMap<>();
            jobData.put("job_id", job.getId());
            jobData.put("title", job.getTitle());
            jobData.put("description", job.getDescription());
            jobData.put("requirements", job.getRequirements());
            jobsData.add(jobData);
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("user_id", userId);
        userData.put("resume_text", resume.getParsedText());
        userData.put("skills", resume.getSkills());
        userData.put("experience_years", resume.getExperienceYears());

        // Call ML service for recommendations
        List<Map<String, Object>> recommendations = mlServiceClient.getRecommendations(userData, jobsData);

        // Map back to Job entities
        List<Job> recommendedJobs = new ArrayList<>();
        for (Map<String, Object> rec : recommendations) {
            Long jobId = ((Number) rec.get("job_id")).longValue();
            jobRepository.findById(jobId).ifPresent(recommendedJobs::add);
            if (recommendedJobs.size() >= limit) break;
        }

        return recommendedJobs;
    }
}