package com.smarthire.controller;

import com.smarthire.dto.JobRecommendationResponse;
import com.smarthire.dto.JobResponseDTO;
import com.smarthire.dto.MatchResponse;
import com.smarthire.model.Job;
import com.smarthire.model.SkillGapRoadmap;
import com.smarthire.model.User;
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
            List<JobResponseDTO> jobDTOs = jobs.stream().map(this::convertToDTO).collect(Collectors.toList());
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
            Job job = jobRepository.findById(id).orElseThrow(() -> new RuntimeException("Job not found"));
            JobResponseDTO jobDTO = convertToDTO(job);
            return ResponseEntity.ok(jobDTO);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createJob(@AuthenticationPrincipal UserDetails userDetails, @RequestBody Job job) {
        try {
            System.out.println("=== Create Job Request ===");
            System.out.println("Recruiter email: " + userDetails.getUsername());

            Long recruiterId = userService.getUserIdByEmail(userDetails.getUsername());
            User recruiter = userService.getUserById(recruiterId);
            job.setRecruiter(recruiter);
            job.setActive(true);
            Job savedJob = jobRepository.save(job);

            System.out.println("Job created with ID: " + savedJob.getId());


            JobResponseDTO jobDTO = convertToDTO(savedJob);
            return ResponseEntity.ok(jobDTO);

        } catch (Exception e) {
            System.err.println("Error creating job: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{jobId}/match")
    public ResponseEntity<?> matchResumeToJob(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long jobId) {
        try {
            System.out.println("=== Match Resume to Job Request ===");
            System.out.println("User email: " + userDetails.getUsername());
            System.out.println("Job ID: " + jobId);

            Long userId = userService.getUserIdByEmail(userDetails.getUsername());
            System.out.println("User ID: " + userId);

            MatchResponse response = jobMatchingService.matchResumeToJob(userId, jobId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error matching resume to job: " + e.getMessage());
            e.printStackTrace();
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

        if (job.getApplications() != null) {
            dto.setApplicationsCount(job.getApplications().size());
        } else {
            dto.setApplicationsCount(0);
        }

        return dto;
    }
}