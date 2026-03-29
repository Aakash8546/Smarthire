// backend/src/main/java/com/smarthire/service/RankingService.java
package com.smarthire.service;

import com.smarthire.dto.RankedCandidate;
import com.smarthire.entity.*;
import com.smarthire.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RankingService {
    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private MLServiceClient mlServiceClient;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Cacheable(value = "rankings", key = "#jobId + '_' + #page + '_' + #size")
    public List<RankedCandidate> getRankedCandidates(Long jobId, int page, int size) {
        log.info("Getting ranked candidates for job: {}", jobId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Application> applications = applicationRepository.findByJobId(jobId, pageable);

        // Prepare candidate data for ML service
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (Application app : applications) {
            Resume resume = app.getResume();
            if (resume != null && resume.getParsedText() != null) {
                Map<String, Object> candidateData = new HashMap<>();
                candidateData.put("user_id", app.getUser().getId());
                candidateData.put("resume_text", resume.getParsedText());
                candidateData.put("experience_years", resume.getExperienceYears());
                candidateData.put("skills", resume.getSkills());
                candidates.add(candidateData);
            }
        }

        // Call ML service for ranking
        List<Map<String, Object>> rankedResults = mlServiceClient.rankCandidates(jobId, candidates);

        // Convert to DTOs
        List<RankedCandidate> rankedCandidates = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> result : rankedResults) {
            RankedCandidate candidate = new RankedCandidate();
            candidate.setUserId(((Number) result.get("user_id")).longValue());
            candidate.setScore(BigDecimal.valueOf((Double) result.get("score")));
            candidate.setExplanation((String) result.get("explanation"));
            candidate.setRank(rank++);
            rankedCandidates.add(candidate);
        }

        // Send to Kafka for async processing
        kafkaTemplate.send("ranking-events", jobId.toString(), rankedCandidates);

        return rankedCandidates;
    }

    @Transactional
    public void updateApplicationScores(Long jobId) {
        List<RankedCandidate> rankings = getRankedCandidates(jobId, 0, 1000);

        for (RankedCandidate ranking : rankings) {
            Application application = applicationRepository
                    .findByJobIdAndUserId(jobId, ranking.getUserId())
                    .orElse(null);

            if (application != null) {
                application.setScore(ranking.getScore());
                applicationRepository.save(application);
            }
        }

        log.info("Updated scores for job {} with {} applications", jobId, rankings.size());
    }
}