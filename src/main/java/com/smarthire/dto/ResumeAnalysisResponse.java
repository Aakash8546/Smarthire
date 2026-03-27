package com.smarthire.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ResumeAnalysisResponse {
    private Long userId;
    private String fileName;
    private List<String> extractedSkills;
    private Integer score;           // Use 'score' for both
    private List<String> suggestions;  // Use 'suggestions' for both
    private LocalDateTime analysisDate;
    private String message;

    public ResumeAnalysisResponse() {}

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<String> getExtractedSkills() {
        return extractedSkills;
    }

    public void setExtractedSkills(List<String> extractedSkills) {
        this.extractedSkills = extractedSkills;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public LocalDateTime getAnalysisDate() {
        return analysisDate;
    }

    public void setAnalysisDate(LocalDateTime analysisDate) {
        this.analysisDate = analysisDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}