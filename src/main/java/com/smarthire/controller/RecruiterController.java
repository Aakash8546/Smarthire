package com.smarthire.controller;

import com.smarthire.model.Application;
import com.smarthire.model.Job;
import com.smarthire.repository.ApplicationRepository;
import com.smarthire.repository.JobRepository;
import com.smarthire.repository.UserRepository;
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
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @GetMapping("/applications")
    public ResponseEntity<?> getApplications(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            var recruiter = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Recruiter not found"));

            List<Application> applications = applicationRepository
                    .findByJobRecruiterOrderByMatchPercentageDesc(recruiter);

            return ResponseEntity.ok(applications);
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
            var recruiter = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Recruiter not found"));

            List<Application> applications = applicationRepository
                    .findByJobRecruiterOrderByMatchPercentageDesc(recruiter);

            List<Application> filtered = applications.stream()
                    .filter(app -> app.getMissingSkills() != null &&
                            app.getMissingSkills().toLowerCase().contains(skill.toLowerCase()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(filtered);
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
            var recruiter = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Recruiter not found"));

            List<Application> applications = applicationRepository
                    .findByRecruiterAndMinMatchPercentage(recruiter, percentage);

            return ResponseEntity.ok(applications);
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
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            // Verify recruiter owns this job
            var recruiter = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Recruiter not found"));

            if (!application.getJob().getRecruiter().getId().equals(recruiter.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Unauthorized to update this application"));
            }

            application.setStatus(status.toUpperCase());
            applicationRepository.save(application);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Application status updated to: " + status);
            response.put("applicationId", applicationId.toString());
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
            var recruiter = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Recruiter not found"));

            List<Job> jobs = jobRepository.findByRecruiter(recruiter);
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }


}