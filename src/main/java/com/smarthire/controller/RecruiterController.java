package com.smarthire.controller;

import com.smarthire.model.Job;
import com.smarthire.model.User;
import com.smarthire.repository.JobRepository;
import com.smarthire.repository.ResumeRepository;
import com.smarthire.repository.UserRepository;
import com.smarthire.service.JobMatchingService;
import com.smarthire.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/recruiter")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecruiterController {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final JobMatchingService matchingService;
    private final JwtUtil jwtUtil;

    @PostMapping("/jobs")
    public ResponseEntity<?> createJob(
            @RequestBody Job job,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            User recruiter = jwtUtil.getUserFromToken(token);

            if (!"RECRUITER".equals(recruiter.getRole())) {
                return ResponseEntity.status(403).body(Map.of("error", "Only recruiters can post jobs"));
            }

            Job savedJob = jobRepository.save(job);
            return ResponseEntity.ok(savedJob);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/candidates")
    public ResponseEntity<?> getCandidatesSortedByMatch(
            @RequestParam String jobId,
            @RequestParam(required = false) String skill,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            User recruiter = jwtUtil.getUserFromToken(token);

            if (!"RECRUITER".equals(recruiter.getRole())) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            List<User> allUsers = userRepository.findAll();
            List<Map<String, Object>> candidates = new ArrayList<>();

            for (User user : allUsers) {
                if ("USER".equals(user.getRole())) {
                    Optional<com.smarthire.model.Resume> resumeOpt = resumeRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId());

                    if (resumeOpt.isPresent()) {
                        var matchResult = matchingService.matchResumeWithJob(resumeOpt.get(), job);

                        Map<String, Object> candidate = new HashMap<>();
                        candidate.put("userId", user.getId());
                        candidate.put("name", user.getName());
                        candidate.put("email", user.getEmail());
                        candidate.put("matchPercentage", matchResult.getMatchPercentage());
                        candidate.put("matchedSkills", matchResult.getMatchedSkills());
                        candidate.put("missingSkills", matchResult.getMissingSkills());

                        if (skill == null || skill.isEmpty() ||
                                matchResult.getMatchedSkills().stream().anyMatch(s -> s.toLowerCase().contains(skill.toLowerCase()))) {
                            candidates.add(candidate);
                        }
                    }
                }
            }

            candidates.sort((a, b) ->
                    ((Integer) b.get("matchPercentage")).compareTo((Integer) a.get("matchPercentage")));

            return ResponseEntity.ok(candidates);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}