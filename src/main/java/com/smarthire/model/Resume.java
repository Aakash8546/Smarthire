package com.smarthire.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "resumes")
@Data
@NoArgsConstructor
@AllArgsConstructor
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