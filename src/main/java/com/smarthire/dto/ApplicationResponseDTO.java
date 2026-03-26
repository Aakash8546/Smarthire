package com.smarthire.dto;

import com.smarthire.model.ApplicationStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApplicationResponseDTO {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private String company;
    private String location;
    private ApplicationStatus status;
    private Integer matchPercentage;
    private String matchingSkills;
    private String missingSkills;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;

    // For recruiter view - add these fields
    private String candidateName;
    private String candidateEmail;
}