package com.smarthire.controller;

import com.smarthire.dto.ApplicationResponseDTO;
import com.smarthire.dto.ApplicationUpdateRequest;
import com.smarthire.model.UserType;
import com.smarthire.service.ApplicationService;
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
@RequestMapping("/api/applications")
@CrossOrigin(origins = "*")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private UserService userService;

    @PostMapping("/apply/{jobId}")
    public ResponseEntity<?> applyForJob(@AuthenticationPrincipal UserDetails userDetails,
                                         @PathVariable Long jobId) {
        try {
            Long userId = userService.getUserIdByEmail(userDetails.getUsername());
            ApplicationResponseDTO application = applicationService.applyForJob(userId, jobId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Application submitted successfully");
            response.put("application", application);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/my-applications")
    public ResponseEntity<?> getMyApplications(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = userService.getUserIdByEmail(userDetails.getUsername());
            List<ApplicationResponseDTO> applications = applicationService.getMyApplications(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applications", applications);
            response.put("total", applications.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/recruiter/applications")
    public ResponseEntity<?> getRecruiterApplications(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = userService.getUserIdByEmail(userDetails.getUsername());
            List<ApplicationResponseDTO> applications = applicationService.getApplicationsForRecruiter(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applications", applications);
            response.put("total", applications.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<?> getApplication(@AuthenticationPrincipal UserDetails userDetails,
                                            @PathVariable Long applicationId) {
        try {
            Long userId = userService.getUserIdByEmail(userDetails.getUsername());
            boolean isRecruiter = userService.getUserByEmail(userDetails.getUsername()).getUserType() == UserType.RECRUITER;

            ApplicationResponseDTO application = applicationService.getApplicationById(applicationId, userId, isRecruiter);

            return ResponseEntity.ok(application);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{applicationId}/status")
    public ResponseEntity<?> updateApplicationStatus(@AuthenticationPrincipal UserDetails userDetails,
                                                     @PathVariable Long applicationId,
                                                     @RequestBody ApplicationUpdateRequest request) {
        try {
            Long recruiterId = userService.getUserIdByEmail(userDetails.getUsername());

            // Check if user is recruiter
            if (userService.getUserById(recruiterId).getUserType() != UserType.RECRUITER) {
                return ResponseEntity.status(403).body(Map.of("error", "Only recruiters can update application status"));
            }

            ApplicationResponseDTO application = applicationService.updateApplicationStatus(applicationId, recruiterId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Application status updated successfully");
            response.put("application", application);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}