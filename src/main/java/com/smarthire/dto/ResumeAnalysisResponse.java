package com.smarthire.dto;

import lombok.Data;
import java.util.List;

@Data
public class ResumeAnalysisResponse {
    private Long resumeId;
    private Integer score;
    private List<String> extractedSkills;
    private List<String> suggestions;
    private String analysisDate;
    private String message;
}