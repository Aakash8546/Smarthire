package com.smarthire.controller;


import com.smarthire.dto.VideoAnalysisResponseDTO;
import com.smarthire.service.VideoAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
@Slf4j
public class VideoController {


    private final VideoAnalysisService videoAnalysisService;

    @PostMapping("/analyze")
    @PreAuthorize("hasAnyRole('RECRUITER', 'CANDIDATE')")
    public ResponseEntity<VideoAnalysisResponseDTO> analyzeVideo(@RequestParam("file") MultipartFile file, @RequestParam(required = false) Long applicationId) throws IOException {

        log.info("Analyzing video for application: {}", applicationId);
        VideoAnalysisResponseDTO response = videoAnalysisService.analyzeVideo(file, applicationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analyze/{interviewId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<VideoAnalysisResponseDTO> getVideoAnalysis(@PathVariable Long interviewId) {
        log.info("Getting video analysis for interview: {}", interviewId);
        VideoAnalysisResponseDTO response = videoAnalysisService.getVideoAnalysis(interviewId);
        return ResponseEntity.ok(response);
    }

}
