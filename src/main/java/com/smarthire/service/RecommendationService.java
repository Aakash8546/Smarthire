
package com.smarthire.service;

import com.smarthire.dto.RecommendationResponseDTO;
import com.smarthire.entity.*;
import com.smarthire.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final MLServiceClient mlServiceClient;
    private final EmbeddingRepository embeddingRepository;

    @Cacheable(value = "recommendations", key = "#userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<RecommendationResponseDTO> getRecommendations(Long userId, Pageable pageable, String sortBy) {
        log.info("Generating recommendations for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        Resume resume = resumeRepository.findWithTextByUserId(userId).orElse(null);

        // Cold start handling
        if (resume == null || resume.getParsedText() == null) {
            log.info("Cold start for user: {}, returning recent jobs", userId);
            return getColdStartRecommendations(pageable);
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
            return Page.empty(pageable);
        }

        // Prepare data for ML service
        Map<String, Object> userData = prepareUserData(user, resume);
        List<Map<String, Object>> jobsData = prepareJobsData(eligibleJobs);

        // Get recommendations from ML service
        List<Map<String, Object>> recommendations = mlServiceClient.getRecommendations(userData, jobsData);

        // Convert to DTOs
        List<RecommendationResponseDTO> recommendationDTOs = recommendations.stream()
                .map(rec -> {
                    Long jobId = ((Number) rec.get("job_id")).longValue();
                    Job job = jobRepository.findById(jobId).orElse(null);
                    if (job == null) return null;

                    return RecommendationResponseDTO.builder()
                            .jobId(jobId)
                            .title(job.getTitle())
                            .description(job.getDescription())
                            .location(job.getLocation())
                            .salaryRange(job.getSalaryRange())
                            .score(BigDecimal.valueOf((Double) rec.get("score")))
                            .reason((String) rec.get("reason"))
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Apply sorting
        sortRecommendations(recommendationDTOs, sortBy);

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), recommendationDTOs.size());

        List<RecommendationResponseDTO> pagedList = recommendationDTOs.subList(start, end);

        return new PageImpl<>(pagedList, pageable, recommendationDTOs.size());
    }

    private Map<String, Object> prepareUserData(User user, Resume resume) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("user_id", user.getId());
        userData.put("resume_text", resume.getParsedText());
        userData.put("skills", resume.getSkills());
        userData.put("experience_years", resume.getExperienceYears());
        userData.put("education_level", resume.getEducationLevel());

        // Get user interaction history
        List<Application> applications = applicationRepository.findByUserId(user.getId());
        List<Long> appliedJobIds = applications.stream()
                .map(app -> app.getJob().getId())
                .collect(Collectors.toList());
        userData.put("applied_job_ids", appliedJobIds);

        return userData;
    }

    private List<Map<String, Object>> prepareJobsData(List<Job> jobs) {
        List<Map<String, Object>> jobsData = new ArrayList<>();

        for (Job job : jobs) {
            Map<String, Object> jobData = new HashMap<>();
            jobData.put("job_id", job.getId());
            jobData.put("title", job.getTitle());
            jobData.put("description", job.getDescription());
            jobData.put("requirements", job.getRequirements());
            jobData.put("location", job.getLocation());
            jobData.put("salary_range", job.getSalaryRange());
            jobsData.add(jobData);
        }

        return jobsData;
    }

    private Page<RecommendationResponseDTO> getColdStartRecommendations(Pageable pageable) {
        Page<Job> recentJobs = jobRepository.findByStatusOrderByCreatedAtDesc("ACTIVE", pageable);

        List<RecommendationResponseDTO> recommendations = recentJobs.stream()
                .map(job -> RecommendationResponseDTO.builder()
                        .jobId(job.getId())
                        .title(job.getTitle())
                        .description(job.getDescription())
                        .location(job.getLocation())
                        .salaryRange(job.getSalaryRange())
                        .score(BigDecimal.valueOf(0.5))
                        .reason("Popular job based on recent activity")
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(recommendations, pageable, recentJobs.getTotalElements());
    }

    private void sortRecommendations(List<RecommendationResponseDTO> recommendations, String sortBy) {
        switch (sortBy) {
            case "score":
                recommendations.sort((a, b) -> b.getScore().compareTo(a.getScore()));
                break;
            case "title":
                recommendations.sort((a, b) -> a.getTitle().compareTo(b.getTitle()));
                break;
            case "date":
                // Would need created date in DTO
                break;
            default:
                recommendations.sort((a, b) -> b.getScore().compareTo(a.getScore()));
        }
    }

    @CacheEvict(value = "recommendations", key = "#userId")
    public void refreshRecommendationsCache(Long userId) {
        log.info("Refreshing recommendations cache for user: {}", userId);
        // Cache will be evicted, next call will regenerate
    }
}