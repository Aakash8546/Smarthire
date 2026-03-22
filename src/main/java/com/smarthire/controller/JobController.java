package com.smarthire.controller;

import com.smarthire.dto.JobRecommendationResponse;
import com.smarthire.dto.MatchResponse;
import com.smarthire.model.Job;
import com.smarthire.model.SkillGapRoadmap;
import com.smarthire.repository.JobRepository;
import com.smarthire.service.JobMatchingService;
import com.smarthire.service.RecommendationService;
import com.smarthire.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobMatchingService jobMatchingService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<?> getAllJobs() {
        try {
            List<Job> jobs = jobRepository.findActiveJobs();
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJobById(@PathVariable Long id) {
        try {
            Job job = jobRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Job not found"));
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createJob(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestBody Job job) {
        try {
            Long recruiterId = userService.getUserByEmail(userDetails.getUsername()).getId();
            job.setRecruiter(userService.getUserById(recruiterId));
            job.setActive(true);
            Job savedJob = jobRepository.save(job);
            return ResponseEntity.ok(savedJob);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{jobId}/match")
    public ResponseEntity<?> matchResumeToJob(@AuthenticationPrincipal UserDetails userDetails,
                                              @PathVariable Long jobId) {
        try {
            Long userId = userService.getUserByEmail(userDetails.getUsername()).getId();
            MatchResponse response = jobMatchingService.matchResumeToJob(userId, jobId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations(@AuthenticationPrincipal UserDetails userDetails,
                                                @RequestParam(defaultValue = "10") int limit) {
        try {
            Long userId = userService.getUserByEmail(userDetails.getUsername()).getId();
            List<JobRecommendationResponse> recommendations =
                    recommendationService.getPersonalizedRecommendations(userId, limit);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{jobId}/roadmap")
    public ResponseEntity<?> generateRoadmap(@AuthenticationPrincipal UserDetails userDetails,
                                             @PathVariable Long jobId) {
        try {
            Long userId = userService.getUserByEmail(userDetails.getUsername()).getId();
            SkillGapRoadmap roadmap = recommendationService.generateSkillGapRoadmap(userId, jobId);
            return ResponseEntity.ok(roadmap);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}