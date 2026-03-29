// backend/src/main/java/com/smarthire/controller/JobController.java
package com.smarthire.controller;

import com.smarthire.dto.RankedCandidate;
import com.smarthire.entity.Job;
import com.smarthire.service.JobService;
import com.smarthire.service.RankingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@Slf4j
public class JobController {
    @Autowired
    private JobService jobService;

    @Autowired
    private RankingService rankingService;

    @GetMapping
    public ResponseEntity<Page<Job>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(jobService.getAllJobs(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Job> createJob(@Valid @RequestBody Job job) {
        return ResponseEntity.ok(jobService.createJob(job));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @Valid @RequestBody Job job) {
        return ResponseEntity.ok(jobService.updateJob(id, job));
    }

    @GetMapping("/{jobId}/candidates/ranked")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<List<RankedCandidate>> getRankedCandidates(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(rankingService.getRankedCandidates(jobId, page, size));
    }

    @PostMapping("/{jobId}/score-update")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Void> updateScores(@PathVariable Long jobId) {
        rankingService.updateApplicationScores(jobId);
        return ResponseEntity.ok().build();
    }
}