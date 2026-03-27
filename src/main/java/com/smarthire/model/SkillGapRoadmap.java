package com.smarthire.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "skill_gap_roadmaps")
public class SkillGapRoadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long jobId;

    @Column(columnDefinition = "TEXT")
    private String roadmap;

    private Integer estimatedWeeks;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public SkillGapRoadmap() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getRoadmap() {
        return roadmap;
    }

    public void setRoadmap(String roadmap) {
        this.roadmap = roadmap;
    }

    public Integer getEstimatedWeeks() {
        return estimatedWeeks;
    }

    public void setEstimatedWeeks(Integer estimatedWeeks) {
        this.estimatedWeeks = estimatedWeeks;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}