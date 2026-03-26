package com.smarthire.controller;

import com.smarthire.dto.ResumeAnalysisResponse;
import com.smarthire.model.User;
import com.smarthire.service.ResumeAnalysisService;
import com.smarthire.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin(origins = "*")
public class ResumeController {

    @Autowired
    private ResumeAnalysisService resumeAnalysisService;

    @Autowired
    private UserService userService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(@AuthenticationPrincipal UserDetails userDetails,
                                          @RequestParam("file") MultipartFile file) {
        try {
            System.out.println("=== Upload Resume Request ===");
            System.out.println("UserDetails email: " + userDetails.getUsername());

            // Get user ID by email
            Long userId = userService.getUserIdByEmail(userDetails.getUsername());
            System.out.println("User ID found: " + userId);

            // If you need the full user object
            // User user = userService.getUserById(userId);
            // System.out.println("User name: " + user.getName());

            if (file.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "File is empty. Please select a file to upload.");
                return ResponseEntity.badRequest().body(error);
            }

            if (file.getSize() > 10 * 1024 * 1024) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "File size exceeds 10MB limit.");
                return ResponseEntity.badRequest().body(error);
            }

            String fileName = file.getOriginalFilename();
            if (fileName != null && !fileName.isEmpty()) {
                String fileExt = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                if (!fileExt.equals("pdf") && !fileExt.equals("docx") && !fileExt.equals("txt")) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Invalid file type. Please upload PDF, DOCX, or TXT files. Got: " + fileExt);
                    return ResponseEntity.badRequest().body(error);
                }
                System.out.println("File type: " + fileExt);
            }

            ResumeAnalysisResponse response = resumeAnalysisService.analyzeResume(userId, file);
            System.out.println("Resume uploaded successfully for user: " + userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error uploading resume: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error processing resume: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/analysis")
    public ResponseEntity<?> getResumeAnalysis(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("=== Get Resume Analysis Request ===");
            System.out.println("UserDetails email: " + userDetails.getUsername());

            Long userId = userService.getUserIdByEmail(userDetails.getUsername());
            System.out.println("User ID: " + userId);

            ResumeAnalysisResponse response = resumeAnalysisService.getResumeAnalysis(userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error getting resume analysis: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/exists")
    public ResponseEntity<?> hasResume(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = userService.getUserIdByEmail(userDetails.getUsername());
            boolean hasResume = resumeAnalysisService.hasResume(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("hasResume", hasResume);
            response.put("userId", userId);
            response.put("email", userDetails.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Example endpoint using getUserById
    @GetMapping("/user/{id}")
    public ResponseEntity<?> getUserResumeInfo(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            boolean hasResume = resumeAnalysisService.hasResume(id);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", id);
            response.put("userName", user.getName());
            response.put("userEmail", user.getEmail());
            response.put("userType", user.getUserType());
            response.put("hasResume", hasResume);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}