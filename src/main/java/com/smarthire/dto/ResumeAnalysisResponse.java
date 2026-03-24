package com.smarthire.dto;

import java.util.List;

public class ResumeAnalysisResponse {
    private Long resumeId;
    private Integer score;
    private List<String> extractedSkills;
    private List<String> suggestions;
    private String analysisDate;
    private String message;

    // Constructors
    public ResumeAnalysisResponse() {}

    // Getters and Setters
    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public List<String> getExtractedSkills() { return extractedSkills; }
    public void setExtractedSkills(List<String> extractedSkills) { this.extractedSkills = extractedSkills; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public String getAnalysisDate() { return analysisDate; }
    public void setAnalysisDate(String analysisDate) { this.analysisDate = analysisDate; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}