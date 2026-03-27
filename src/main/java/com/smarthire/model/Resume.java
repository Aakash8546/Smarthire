package com.smarthire.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)  // Add this relationship
    private User user;  // Change from userId to User object

    private String fileName;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String extractedSkills;

    private Integer aiScore;

    @Column(columnDefinition = "TEXT")
    private String skillSuggestions;

    private LocalDateTime analysisDate;
    private LocalDateTime createdAt;

    // Constructors
    public Resume() {}

    // Add these methods for skill list conversion
    public List<String> getSkillsList() {
        if (extractedSkills == null || extractedSkills.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(extractedSkills.split(","));
    }

    public List<String> getSkillSuggestionsList() {
        if (skillSuggestions == null || skillSuggestions.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(skillSuggestions.split(","));
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExtractedSkills() {
        return extractedSkills;
    }

    public void setExtractedSkills(String extractedSkills) {
        this.extractedSkills = extractedSkills;
    }

    public Integer getAiScore() {
        return aiScore;
    }

    public void setAiScore(Integer aiScore) {
        this.aiScore = aiScore;
    }

    public String getSkillSuggestions() {
        return skillSuggestions;
    }

    public void setSkillSuggestions(String skillSuggestions) {
        this.skillSuggestions = skillSuggestions;
    }

    public LocalDateTime getAnalysisDate() {
        return analysisDate;
    }

    public void setAnalysisDate(LocalDateTime analysisDate) {
        this.analysisDate = analysisDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}