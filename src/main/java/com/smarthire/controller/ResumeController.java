package com.smarthire.controller;

import com.smarthire.dto.ResumeAnalysisResponse;
import com.smarthire.model.User;
import com.smarthire.service.ResumeAnalysisService;
import com.smarthire.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ResumeController {

    private final ResumeAnalysisService resumeAnalysisService;
    private final JwtUtil jwtUtil;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            User user = jwtUtil.getUserFromToken(token);

            ResumeAnalysisResponse response = resumeAnalysisService.analyzeResume(file, user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}