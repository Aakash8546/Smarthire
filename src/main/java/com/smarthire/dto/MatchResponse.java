package com.smarthire.dto;

import lombok.Data;
import java.util.List;

@Data
public class MatchResponse {
    private Long jobId;
    private String jobTitle;
    private String company;
    private Integer matchPercentage;
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private String recommendation;
    private boolean alreadyApplied;
}