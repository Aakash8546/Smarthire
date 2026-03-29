
package com.smarthire.controller;

import com.smarthire.dto.InterviewEvaluationRequestDTO;
import com.smarthire.dto.InterviewEvaluationResponseDTO;
import com.smarthire.service.InterviewEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
@Slf4j
public class InterviewController {

    private final InterviewEvaluationService interviewEvaluationService;

    @PostMapping("/evaluate")
    @PreAuthorize("hasAnyRole('RECRUITER', 'CANDIDATE')")
    public ResponseEntity<InterviewEvaluationResponseDTO> evaluateInterview(
            @Valid @RequestBody InterviewEvaluationRequestDTO request) {
        log.info("Evaluating interview for application: {}", request.getApplicationId());
        InterviewEvaluationResponseDTO response =
                interviewEvaluationService.evaluateInterview(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evaluate/{applicationId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<InterviewEvaluationResponseDTO> getEvaluation(
            @PathVariable Long applicationId) {
        log.info("Getting interview evaluation for application: {}", applicationId);
        InterviewEvaluationResponseDTO response =
                interviewEvaluationService.getEvaluation(applicationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/evaluate/batch")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Void> batchEvaluateInterviews(
            @RequestParam Long jobId) {
        log.info("Batch evaluating interviews for job: {}", jobId);
        interviewEvaluationService.batchEvaluateInterviews(jobId);
        return ResponseEntity.ok().build();
    }
}