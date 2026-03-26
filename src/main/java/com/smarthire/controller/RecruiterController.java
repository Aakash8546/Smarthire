package com.smarthire.controller;

import com.smarthire.model.Application;
import com.smarthire.model.ApplicationStatus;
import com.smarthire.model.Job;
import com.smarthire.repository.ApplicationRepository;
import com.smarthire.repository.JobRepository;
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
@RequestMapping("/api/recruiter")
@CrossOrigin(origins = "*")
public class RecruiterController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserService userService;

    @GetMapping("/applications")
    public ResponseEntity<?> getApplications(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long recruiterId = userService.getUserIdByEmail(userDetails.getUsername());

            List<Application> applications = applicationRepository
                    .findByJobRecruiterIdOrderByMatchPercentageDesc(recruiterId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applications", applications);
            response.put("total", applications.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/applications/skill/{skill}")
    public ResponseEntity<?> filterBySkill(@AuthenticationPrincipal UserDetails userDetails,
                                           @PathVariable String skill) {
        try {
            Long recruiterId = userService.getUserIdByEmail(userDetails.getUsername());

            List<Application> applications = applicationRepository
                    .findByJobRecruiterIdOrderByMatchPercentageDesc(recruiterId);

            List<Application> filtered = applications.stream()
                    .filter(app -> app.getMissingSkills() != null &&
                            app.getMissingSkills().toLowerCase().contains(skill.toLowerCase()))
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applications", filtered);
            response.put("total", filtered.size());
            response.put("skill", skill);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/applications/min-match/{percentage}")
    public ResponseEntity<?> filterByMinMatchPercentage(@AuthenticationPrincipal UserDetails userDetails,
                                                        @PathVariable int percentage) {
        try {
            Long recruiterId = userService.getUserIdByEmail(userDetails.getUsername());

            List<Application> applications = applicationRepository
                    .findByJobRecruiterIdOrderByMatchPercentageDesc(recruiterId);

            List<Application> filtered = applications.stream()
                    .filter(app -> app.getMatchPercentage() >= percentage)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applications", filtered);
            response.put("total", filtered.size());
            response.put("minMatchPercentage", percentage);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/applications/{applicationId}/status")
    public ResponseEntity<?> updateApplicationStatus(@AuthenticationPrincipal UserDetails userDetails,
                                                     @PathVariable Long applicationId,
                                                     @RequestParam String status) {
        try {
            Long recruiterId = userService.getUserIdByEmail(userDetails.getUsername());

            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            // Verify recruiter owns this job
            if (!application.getJob().getRecruiter().getId().equals(recruiterId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Unauthorized to update this application"));
            }

            // Convert string to enum
            ApplicationStatus newStatus;
            try {
                newStatus = ApplicationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid status. Valid statuses: PENDING, REVIEWING, SHORTLISTED, ACCEPTED, REJECTED, INTERVIEW_SCHEDULED");
                return ResponseEntity.badRequest().body(error);
            }

            application.setStatus(newStatus);
            applicationRepository.save(application);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Application status updated to: " + newStatus.getDisplayName());
            response.put("applicationId", applicationId);
            response.put("newStatus", newStatus.name());
            response.put("statusDisplayName", newStatus.getDisplayName());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/jobs")
    public ResponseEntity<?> getMyJobs(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long recruiterId = userService.getUserIdByEmail(userDetails.getUsername());

            List<Job> jobs = jobRepository.findByRecruiterId(recruiterId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobs", jobs);
            response.put("total", jobs.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long recruiterId = userService.getUserIdByEmail(userDetails.getUsername());

            List<Application> applications = applicationRepository
                    .findByJobRecruiterIdOrderByMatchPercentageDesc(recruiterId);

            Map<String, Object> stats = new HashMap<>();
            stats.put("success", true);
            stats.put("totalApplications", applications.size());
            stats.put("averageMatchPercentage", applications.stream()
                    .mapToInt(Application::getMatchPercentage)
                    .average()
                    .orElse(0));
            stats.put("shortlisted", applications.stream()
                    .filter(a -> a.getStatus() == ApplicationStatus.SHORTLISTED)
                    .count());
            stats.put("pending", applications.stream()
                    .filter(a -> a.getStatus() == ApplicationStatus.PENDING)
                    .count());
            stats.put("accepted", applications.stream()
                    .filter(a -> a.getStatus() == ApplicationStatus.ACCEPTED)
                    .count());
            stats.put("rejected", applications.stream()
                    .filter(a -> a.getStatus() == ApplicationStatus.REJECTED)
                    .count());
            stats.put("reviewing", applications.stream()
                    .filter(a -> a.getStatus() == ApplicationStatus.REVIEWING)
                    .count());
            stats.put("interviewScheduled", applications.stream()
                    .filter(a -> a.getStatus() == ApplicationStatus.INTERVIEW_SCHEDULED)
                    .count());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}