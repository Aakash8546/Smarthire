package com.smarthire.controller;

import com.smarthire.dto.JobRecommendationResponse;
import com.smarthire.dto.JobResponseDTO;
import com.smarthire.dto.MatchResponse;
import com.smarthire.model.Job;
import com.smarthire.model.SkillGapRoadmap;
import com.smarthire.model.User;
import com.smarthire.model.UserType;
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
import java.util.stream.Collectors;

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
            List<JobResponseDTO> jobDTOs = jobs.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(jobDTOs);
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
            JobResponseDTO jobDTO = convertToDTO(job);
            return ResponseEntity.ok(jobDTO);
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
            System.out.println("=== Create Job Request ===");
            System.out.println("Recruiter email: " + userDetails.getUsername());

            Long recruiterId = userService.getUserIdByEmail(userDetails.getUsername());
            User recruiter = userService.getUserById(recruiterId);

            if (recruiter.getUserType() != UserType.RECRUITER) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Only recruiters can create jobs");
                return ResponseEntity.status(403).body(error);
            }

            job.setRecruiter(recruiter);
            job.setActive(true);
            Job savedJob = jobRepository.save(job);
            System.out.println("Job created successfully with ID: " + savedJob.getId());

            JobResponseDTO jobDTO = convertToDTO(savedJob);
            return ResponseEntity.ok(jobDTO);

        } catch (Exception e) {
            System.err.println("Error creating job: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations(@AuthenticationPrincipal UserDetails userDetails,
                                                @RequestParam(defaultValue = "10") int limit) {
        try {
            Long userId = userService.getUserIdByEmail(userDetails.getUsername());
            User user = userService.getUserById(userId);

            if (user.getUserType() != UserType.CANDIDATE) {
                return ResponseEntity.status(403).body(Map.of("error", "Only candidates can get job recommendations"));
            }

            List<JobRecommendationResponse> recommendations = recommendationService.getPersonalizedRecommendations(userId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("recommendations", recommendations);
            response.put("total", recommendations.size());
            response.put("userName", user.getName());

            return ResponseEntity.ok(response);
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
            System.out.println("=== Match Resume to Job Request ===");
            System.out.println("User email: " + userDetails.getUsername());
            System.out.println("Job ID: " + jobId);

            Long userId = userService.getUserIdByEmail(userDetails.getUsername());
            User user = userService.getUserById(userId);

            if (user.getUserType() != UserType.CANDIDATE) {
                return ResponseEntity.status(403).body(Map.of("error", "Only candidates can match resumes to jobs"));
            }

            MatchResponse response = jobMatchingService.matchResumeToJob(userId, jobId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error matching resume to job: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{jobId}/roadmap")
    public ResponseEntity<?> generateRoadmap(@AuthenticationPrincipal UserDetails userDetails,
                                             @PathVariable Long jobId) {
        try {
            Long userId = userService.getUserIdByEmail(userDetails.getUsername());
            User user = userService.getUserById(userId);

            if (user.getUserType() != UserType.CANDIDATE) {
                return ResponseEntity.status(403).body(Map.of("error", "Only candidates can generate skill gap roadmaps"));
            }

            SkillGapRoadmap roadmap = recommendationService.generateSkillGapRoadmap(userId, jobId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", roadmap.getId());
            response.put("userId", roadmap.getUserId());
            response.put("jobId", roadmap.getJobId());
            response.put("roadmap", roadmap.getRoadmap());
            response.put("estimatedWeeks", roadmap.getEstimatedWeeks());
            response.put("createdAt", roadmap.getCreatedAt());
            response.put("userName", user.getName());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/my-jobs")
    public ResponseEntity<?> getMyJobs(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long recruiterId = userService.getUserIdByEmail(userDetails.getUsername());
            User recruiter = userService.getUserById(recruiterId);

            if (recruiter.getUserType() != UserType.RECRUITER) {
                return ResponseEntity.status(403).body(Map.of("error", "Only recruiters can view their jobs"));
            }

            List<Job> jobs = jobRepository.findByRecruiterId(recruiterId);
            List<JobResponseDTO> jobDTOs = jobs.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("jobs", jobDTOs);
            response.put("total", jobDTOs.size());
            response.put("recruiterName", recruiter.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/recruiter/stats")
    public ResponseEntity<?> getRecruiterStats(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long recruiterId = userService.getUserIdByEmail(userDetails.getUsername());
            User recruiter = userService.getUserById(recruiterId);

            if (recruiter.getUserType() != UserType.RECRUITER) {
                return ResponseEntity.status(403).body(Map.of("error", "Only recruiters can view stats"));
            }

            List<Job> jobs = jobRepository.findByRecruiterId(recruiterId);
            long totalJobs = jobs.size();
            long activeJobs = jobs.stream().filter(Job::isActive).count();
            long totalApplications = jobs.stream()
                    .mapToInt(job -> job.getApplications() != null ? job.getApplications().size() : 0)
                    .sum();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalJobs", totalJobs);
            stats.put("activeJobs", activeJobs);
            stats.put("totalApplications", totalApplications);
            stats.put("recruiterName", recruiter.getName());
            stats.put("recruiterEmail", recruiter.getEmail());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private JobResponseDTO convertToDTO(Job job) {
        JobResponseDTO dto = new JobResponseDTO();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setDescription(job.getDescription());
        dto.setCompany(job.getCompany());
        dto.setLocation(job.getLocation());
        dto.setRequiredSkills(job.getRequiredSkillsList());
        dto.setExperienceYears(job.getExperienceYears());
        dto.setSalaryRange(job.getSalaryRange());
        dto.setActive(job.isActive());
        dto.setCreatedAt(job.getCreatedAt());

        if (job.getRecruiter() != null) {
            dto.setRecruiterId(job.getRecruiter().getId());
            dto.setRecruiterName(job.getRecruiter().getName());
            dto.setRecruiterEmail(job.getRecruiter().getEmail());
        }

        dto.setApplicationsCount(0);
        return dto;
    }
}