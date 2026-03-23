package com.smarthire.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore  // Prevent recursion
    private User user;

    @ManyToOne
    @JoinColumn(name = "job_id")
    @JsonIgnore  // Prevent recursion
    private Job job;

    private Integer matchPercentage;

    @Column(columnDefinition = "TEXT")
    private String missingSkills;

    @Column(name = "matching_skills", columnDefinition = "TEXT")
    private String matchingSkills;

    @Column(name = "application_status")
    private String status = "PENDING";

    @CreationTimestamp
    private LocalDateTime appliedAt;
}