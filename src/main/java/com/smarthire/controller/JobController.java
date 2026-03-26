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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllJobs() {
        try {
            // Use the query that fetches jobs with application counts
            List<Object[]> results = jobRepository.findAllActiveJobsWithApplicationCount();
            List<JobResponseDTO> jobDTOs = new ArrayList<>();

            for (Object[] result : results) {
                Job job = (Job) result[0];
                Long applicationCount = (Long) result[1];
                JobResponseDTO dto = convertToDTO(job);
                dto.setApplicationsCount(applicationCount.intValue());
                jobDTOs.add(dto);
            }

            return ResponseEntity.ok(jobDTOs);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getJobById(@PathVariable Long id) {
        try {
            // Fetch job with application count
            Object result = jobRepository.findJobWithApplicationCount(id);
            if (result == null) {
                return ResponseEntity.notFound().build();
            }

            Object[] resultArray = (Object[]) result;
            Job job = (Job) resultArray[0];
            Long applicationCount = (Long) resultArray[1];

            JobResponseDTO jobDTO = convertToDTO(job);
            jobDTO.setApplicationsCount(applicationCount.intValue());

            return ResponseEntity.ok(jobDTO);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/create")
    @Transactional
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
                error.put("userType", recruiter.getUserType().name());
                return ResponseEntity.status(403).body(error);
            }

            job.setRecruiter(recruiter);
            job.setActive(true);

            Job savedJob = jobRepository.save(job);
            System.out.println("Job created successfully with ID: " + savedJob.getId());

            JobResponseDTO jobDTO = convertToDTO(savedJob);
            jobDTO.setApplicationsCount(0); // New job has 0 applications

            return ResponseEntity.ok(jobDTO);

        } catch (Exception e) {
            System.err.println("Error creating job: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/my-jobs")
    @Transactional(readOnly = true)
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

    // Update other methods to use @Transactional where needed
    @PutMapping("/{jobId}")
    @Transactional
    public ResponseEntity<?> updateJob(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Long jobId,
                                       @RequestBody Job updatedJob) {
        try {
            Long recruiterId = userService.getUserIdByEmail(userDetails.getUsername());
            User recruiter = userService.getUserById(recruiterId);

            if (recruiter.getUserType() != UserType.RECRUITER) {
                return ResponseEntity.status(403).body(Map.of("error", "Only recruiters can update jobs"));
            }

            Job existingJob = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            if (!existingJob.getRecruiter().getId().equals(recruiterId)) {
                return ResponseEntity.status(403).body(Map.of("error", "You can only update your own jobs"));
            }

            existingJob.setTitle(updatedJob.getTitle());
            existingJob.setDescription(updatedJob.getDescription());
            existingJob.setCompany(updatedJob.getCompany());
            existingJob.setLocation(updatedJob.getLocation());
            existingJob.setRequiredSkills(updatedJob.getRequiredSkills());
            existingJob.setExperienceYears(updatedJob.getExperienceYears());
            existingJob.setSalaryRange(updatedJob.getSalaryRange());

            Job savedJob = jobRepository.save(existingJob);
            JobResponseDTO jobDTO = convertToDTO(savedJob);

            return ResponseEntity.ok(jobDTO);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{jobId}")
    @Transactional
    public ResponseEntity<?> deleteJob(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Long jobId) {
        try {
            Long recruiterId = userService.getUserIdByEmail(userDetails.getUsername());
            User recruiter = userService.getUserById(recruiterId);

            if (recruiter.getUserType() != UserType.RECRUITER) {
                return ResponseEntity.status(403).body(Map.of("error", "Only recruiters can delete jobs"));
            }

            Job existingJob = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            if (!existingJob.getRecruiter().getId().equals(recruiterId)) {
                return ResponseEntity.status(403).body(Map.of("error", "You can only delete your own jobs"));
            }

            existingJob.setActive(false);
            jobRepository.save(existingJob);

            return ResponseEntity.ok(Map.of("message", "Job deleted successfully"));

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/recruiter/stats")
    @Transactional(readOnly = true)
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

            // Calculate total applications safely
            long totalApplications = 0;
            for (Job job : jobs) {
                if (job.getApplications() != null) {
                    totalApplications += job.getApplications().size();
                }
            }

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

        // Set default applications count
        dto.setApplicationsCount(0);

        return dto;
    }
}