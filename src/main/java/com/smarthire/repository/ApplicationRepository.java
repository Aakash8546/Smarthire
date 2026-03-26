package com.smarthire.repository;

import com.smarthire.model.Application;
import com.smarthire.model.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUserId(Long userId);

    List<Application> findByJobId(Long jobId);

    List<Application> findByJobRecruiterId(Long recruiterId);

    Optional<Application> findByUserIdAndJobId(Long userId, Long jobId);

    boolean existsByUserIdAndJobId(Long userId, Long jobId);

    @Query("SELECT a FROM Application a WHERE a.job.recruiter.id = :recruiterId ORDER BY a.appliedAt DESC")
    List<Application> findApplicationsByRecruiterId(@Param("recruiterId") Long recruiterId);

    List<Application> findByUserIdAndStatus(Long userId, ApplicationStatus status);

    long countByJobIdAndStatus(Long jobId, ApplicationStatus status);

    // Add this method - for recruiter to get applications ordered by match percentage using recruiterId
    @Query("SELECT a FROM Application a WHERE a.job.recruiter.id = :recruiterId ORDER BY a.matchPercentage DESC")
    List<Application> findByJobRecruiterIdOrderByMatchPercentageDesc(@Param("recruiterId") Long recruiterId);
}