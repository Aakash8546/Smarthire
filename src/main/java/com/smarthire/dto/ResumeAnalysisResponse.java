package com.smarthire.dto;

import lombok.Data;
import java.util.List;

@Data
public class ResumeAnalysisResponse {
    private Long resumeId;  // ✅ String → Long
    private List<String> skills;
    private Integer score;
    private String suggestions;
    private List<String> improvementAreas;
}