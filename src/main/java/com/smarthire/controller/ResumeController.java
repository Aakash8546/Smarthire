package com.smarthire.controller;

import com.smarthire.dto.ResumeAnalysisResponse;
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
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            if (!file.getContentType().equals("application/pdf") &&
                    !file.getContentType().equals("application/msword") &&
                    !file.getContentType().equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") &&
                    !file.getContentType().equals("text/plain")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid file type. Please upload PDF, DOC, DOCX, or TXT file."));
            }

            Long userId = userService.getUserByEmail(userDetails.getUsername()).getId();
            ResumeAnalysisResponse response = resumeAnalysisService.analyzeResume(userId, file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error processing resume: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/analysis")
    public ResponseEntity<?> getResumeAnalysis(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = userService.getUserByEmail(userDetails.getUsername()).getId();
            ResumeAnalysisResponse response = resumeAnalysisService.getResumeAnalysis(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/exists")
    public ResponseEntity<?> hasResume(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = userService.getUserByEmail(userDetails.getUsername()).getId();
            boolean hasResume = resumeAnalysisService.hasResume(userId);
            Map<String, Boolean> response = new HashMap<>();
            response.put("hasResume", hasResume);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}