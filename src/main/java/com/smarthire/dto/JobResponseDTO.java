package com.smarthire.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobResponseDTO {
    private Long id;
    private String title;
    private String description;
    private String company;
    private String location;
    private List<String> requiredSkills;
    private Integer experienceYears;
    private String salaryRange;
    private Long recruiterId;
    private String recruiterName;
    private String recruiterEmail;
    private boolean isActive;
    private LocalDateTime createdAt;
    private Integer applicationsCount;
}