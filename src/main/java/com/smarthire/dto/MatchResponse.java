package com.smarthire.dto;

import lombok.Data;
import java.util.List;

@Data
public class MatchResponse {
    private Integer matchPercentage;
    private List<String> missingSkills;
    private List<String> matchedSkills;
    private String recommendation;
}