package com.smarthire.service;

import com.smarthire.dto.ApplicationResponseDTO;
import com.smarthire.dto.ApplicationUpdateRequest;
import com.smarthire.dto.MatchResponse;
import com.smarthire.model.*;
import com.smarthire.repository.ApplicationRepository;
import com.smarthire.repository.JobRepository;
import com.smarthire.repository.ResumeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private JobMatchingService jobMatchingService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    @Transactional
    public ApplicationResponseDTO applyForJob(Long userId, Long jobId) {
        // Check if user already applied
        if (applicationRepository.existsByUserIdAndJobId(userId, jobId)) {
            throw new RuntimeException("You have already applied for this job");
        }

        // Get user
        User user = userService.getUserById(userId);

        // Get job
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Check if job is active
        if (!job.isActive()) {
            throw new RuntimeException("This job is no longer accepting applications");
        }

        // Check if resume exists
        Resume resume = resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Please upload your resume first"));

        // Get match result
        MatchResponse match = jobMatchingService.matchResumeToJob(userId, jobId);

        // Create application
        Application application = new Application();
        application.setUser(user);
        application.setJob(job);
        application.setMatchPercentage(match.getMatchPercentage());
        application.setMatchingSkills(String.join(",", match.getMatchingSkills()));
        application.setMissingSkills(String.join(",", match.getMissingSkills()));
        application.setStatus(ApplicationStatus.PENDING);

        Application savedApplication = applicationRepository.save(application);

        // Send email confirmation to candidate
        try {
            emailService.sendApplicationConfirmation(
                    user.getEmail(),
                    user.getName(),
                    job.getTitle(),
                    job.getCompany(),
                    match.getMatchPercentage()
            );
        } catch (Exception e) {
            System.err.println("Failed to send confirmation email: " + e.getMessage());
        }

        // Send email notification to recruiter
        try {
            emailService.sendNewApplicationNotification(
                    job.getRecruiter().getEmail(),
                    job.getRecruiter().getName(),
                    job.getTitle(),
                    user.getName(),
                    match.getMatchPercentage()
            );
        } catch (Exception e) {
            System.err.println("Failed to send notification email: " + e.getMessage());
        }

        return convertToDTO(savedApplication);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponseDTO> getMyApplications(Long userId) {
        List<Application> applications = applicationRepository.findByUserId(userId);
        return applications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponseDTO> getApplicationsForRecruiter(Long recruiterId) {
        List<Application> applications = applicationRepository.findApplicationsByRecruiterId(recruiterId);
        return applications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApplicationResponseDTO getApplicationById(Long applicationId, Long userId, boolean isRecruiter) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!isRecruiter && !application.getUser().getId().equals(userId)) {
            throw new RuntimeException("You can only view your own applications");
        }

        if (isRecruiter && !application.getJob().getRecruiter().getId().equals(userId)) {
            throw new RuntimeException("You can only view applications for your jobs");
        }

        return convertToDTO(application);
    }

    @Transactional
    public ApplicationResponseDTO updateApplicationStatus(Long applicationId, Long recruiterId,
                                                          ApplicationUpdateRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Verify recruiter owns this job
        if (!application.getJob().getRecruiter().getId().equals(recruiterId)) {
            throw new RuntimeException("You can only update applications for your jobs");
        }

        application.setStatus(request.getStatus());
        application.setRecruiterNotes(request.getNotes());

        Application updatedApplication = applicationRepository.save(application);

        // Send email notification to candidate about status change
        try {
            emailService.sendApplicationStatusUpdate(
                    application.getUser().getEmail(),
                    application.getUser().getName(),
                    application.getJob().getTitle(),
                    application.getJob().getCompany(),
                    request.getStatus().getDisplayName(),
                    request.getNotes()
            );
        } catch (Exception e) {
            System.err.println("Failed to send status update email: " + e.getMessage());
        }

        return convertToDTO(updatedApplication);
    }

    @Transactional(readOnly = true)
    public long getApplicationCountForJob(Long jobId, ApplicationStatus status) {
        return applicationRepository.countByJobIdAndStatus(jobId, status);
    }

    @Transactional(readOnly = true)
    public boolean hasApplied(Long userId, Long jobId) {
        return applicationRepository.existsByUserIdAndJobId(userId, jobId);
    }

    private ApplicationResponseDTO convertToDTO(Application application) {
        ApplicationResponseDTO dto = new ApplicationResponseDTO();
        dto.setId(application.getId());
        dto.setJobId(application.getJob().getId());
        dto.setJobTitle(application.getJob().getTitle());
        dto.setCompany(application.getJob().getCompany());
        dto.setLocation(application.getJob().getLocation());
        dto.setStatus(application.getStatus());
        dto.setMatchPercentage(application.getMatchPercentage());
        dto.setMatchingSkills(application.getMatchingSkills());
        dto.setMissingSkills(application.getMissingSkills());
        dto.setAppliedAt(application.getAppliedAt());
        dto.setUpdatedAt(application.getUpdatedAt());

        // Add candidate info for recruiter view
        if (application.getUser() != null) {
            dto.setCandidateName(application.getUser().getName());
            dto.setCandidateEmail(application.getUser().getEmail());
        }

        return dto;
    }
}