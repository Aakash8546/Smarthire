package com.smarthire.dto;

import lombok.Data;
import java.util.List;

@Data
public class JobRecommendationResponse {
    private Long jobId;
    private String title;
    private String company;
    private String location;
    private Integer matchPercentage;
    private List<String> requiredSkills;
    private List<String> userSkills;
    private Integer missingSkillCount;
    private String salaryRange;
}