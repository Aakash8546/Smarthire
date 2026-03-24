package com.smarthire.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "resumes")
public class Resume {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "extracted_skills", columnDefinition = "TEXT")
    private String extractedSkills;

    @Column(name = "ai_score")
    private Integer aiScore;

    @Column(name = "skill_suggestions", columnDefinition = "TEXT")
    private String skillSuggestions;

    @Column(name = "analysis_date")
    private LocalDateTime analysisDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Resume() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getExtractedSkills() { return extractedSkills; }
    public void setExtractedSkills(String extractedSkills) { this.extractedSkills = extractedSkills; }

    public Integer getAiScore() { return aiScore; }
    public void setAiScore(Integer aiScore) { this.aiScore = aiScore; }

    public String getSkillSuggestions() { return skillSuggestions; }
    public void setSkillSuggestions(String skillSuggestions) { this.skillSuggestions = skillSuggestions; }

    public LocalDateTime getAnalysisDate() { return analysisDate; }
    public void setAnalysisDate(LocalDateTime analysisDate) { this.analysisDate = analysisDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<String> getSkillsList() {
        if (extractedSkills != null && !extractedSkills.isEmpty()) {
            return Arrays.asList(extractedSkills.split(","));
        }
        return new ArrayList<>();
    }

    public List<String> getSuggestionsList() {
        if (skillSuggestions != null && !skillSuggestions.isEmpty()) {
            return Arrays.asList(skillSuggestions.split("\\|\\|"));
        }
        return new ArrayList<>();
    }

    public void setSkillsList(List<String> skills) {
        this.extractedSkills = String.join(",", skills);
    }

    public void setSuggestionsList(List<String> suggestions) {
        this.skillSuggestions = String.join("||", suggestions);
    }
}