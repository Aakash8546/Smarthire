
package com.smarthire.controller;

import com.smarthire.dto.FraudCheckRequestDTO;
import com.smarthire.dto.FraudCheckResponseDTO;
import com.smarthire.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
@Slf4j
public class FraudController {

    private final FraudDetectionService fraudDetectionService;

    @PostMapping("/fraud-check")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<FraudCheckResponseDTO> checkResumeFraud(
            @Valid @RequestBody FraudCheckRequestDTO request) {
        log.info("Checking resume fraud for resume ID: {}", request.getResumeId());
        FraudCheckResponseDTO response = fraudDetectionService.checkFraud(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/fraud-check/upload")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<FraudCheckResponseDTO> checkResumeFraudUpload(
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Checking uploaded resume for fraud");
        FraudCheckResponseDTO response = fraudDetectionService.checkFraudFromFile(file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fraud-check/{applicationId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<FraudCheckResponseDTO> getFraudCheck(@PathVariable Long applicationId) {
        log.info("Getting fraud check for application: {}", applicationId);
        FraudCheckResponseDTO response = fraudDetectionService.getFraudCheck(applicationId);
        return ResponseEntity.ok(response);
    }
}