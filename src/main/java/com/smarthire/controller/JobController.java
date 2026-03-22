package com.smarthire.controller;

import com.smarthire.dto.MatchResult;
import com.smarthire.model.Job;
import com.smarthire.model.Resume;
import com.smarthire.model.User;
import com.smarthire.repository.JobRepository;
import com.smarthire.repository.ResumeRepository;
import com.smarthire.service.JobMatchingService;
import com.smarthire.service.JobRecommendationService;
import com.smarthire.service.SkillGapService;
import com.smarthire.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobController {

    private final JobRepository jobRepository;
    private final JobMatchingService matchingService;
    private final JobRecommendationService recommendationService;
    private final SkillGapService skillGapService;
    private final ResumeRepository resumeRepository;
    private final JwtUtil jwtUtil;

    @GetMapping("/all")
    public ResponseEntity<?> getAllJobs() {
        return ResponseEntity.ok(jobRepository.findAll());
    }

    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            User user = jwtUtil.getUserFromToken(token);

            Resume latestResume = resumeRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                    .orElseThrow(() -> new RuntimeException("No resume found. Please upload resume first."));

            List<MatchResult> recommendations = recommendationService.getPersonalizedRecommendations(latestResume);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/match/{jobId}")
    public ResponseEntity<?> matchWithJob(
            @PathVariable String jobId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            User user = jwtUtil.getUserFromToken(token);

            Resume latestResume = resumeRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                    .orElseThrow(() -> new RuntimeException("No resume found"));

            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            var matchResponse = matchingService.matchResumeWithJob(latestResume, job);
            return ResponseEntity.ok(matchResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/skill-gap")
    public ResponseEntity<?> getSkillGapRoadmap(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            User user = jwtUtil.getUserFromToken(token);

            Resume latestResume = resumeRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                    .orElseThrow(() -> new RuntimeException("No resume found"));

            List<String> targetSkills = List.of("Docker", "Kubernetes", "AWS", "Microservices");
            String roadmap = skillGapService.generateLearningRoadmap(targetSkills, latestResume);
            return ResponseEntity.ok(Map.of("roadmap", roadmap));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}