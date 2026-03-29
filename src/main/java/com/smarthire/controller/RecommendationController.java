
package com.smarthire.controller;

import com.smarthire.dto.RecommendationResponseDTO;
import com.smarthire.entity.Job;
import com.smarthire.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{id}/recommendations")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER')")
    public ResponseEntity<Page<RecommendationResponseDTO>> getRecommendations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "score") String sortBy) {

        log.info("Getting recommendations for candidate: {}", id);
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<RecommendationResponseDTO> recommendations =
                recommendationService.getRecommendations(id, pageRequest, sortBy);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/{id}/recommendations/refresh")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Void> refreshRecommendations(@PathVariable Long id) {
        log.info("Refreshing recommendations for candidate: {}", id);
        recommendationService.refreshRecommendationsCache(id);
        return ResponseEntity.ok().build();
    }
}