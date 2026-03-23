package com.smarthire.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String company;

    private String location;

    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkills;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "salary_range")
    private String salaryRange;

    @ManyToOne
    @JoinColumn(name = "recruiter_id")
    @JsonIgnore  // Add this to prevent infinite recursion
    private User recruiter;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  // Add this to prevent infinite recursion
    private List<Application> applications = new ArrayList<>();

    @Column(name = "is_active")
    private boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public List<String> getRequiredSkillsList() {
        if (requiredSkills != null && !requiredSkills.isEmpty()) {
            return Arrays.asList(requiredSkills.split(","));
        }
        return new ArrayList<>();
    }

    public void setRequiredSkillsList(List<String> skills) {
        this.requiredSkills = String.join(",", skills);
    }
}