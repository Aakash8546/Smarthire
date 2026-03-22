package com.smarthire.dto;

import lombok.Data;
import java.util.List;

@Data
public class MatchResult {
    private Long jobId;  // ✅ String → Long
    private String jobTitle;
    private String company;
    private Integer matchPercentage;
    private List<String> missingSkills;
    private List<String> matchedSkills;
}