package com.smarthire.repository;

import com.smarthire.model.Job;
import com.smarthire.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByRecruiter(User recruiter);

    List<Job> findByRecruiterId(Long recruiterId);

    List<Job> findByIsActiveTrue();

    @Query("SELECT j FROM Job j WHERE j.isActive = true ORDER BY j.createdAt DESC")
    List<Job> findActiveJobs();

    @Query("SELECT j FROM Job j WHERE j.isActive = true AND j.title LIKE %:keyword% OR j.description LIKE %:keyword%")
    List<Job> searchJobs(String keyword);

    // Add this query to fetch jobs with applications count
    @Query("SELECT j, COUNT(a) as applicationCount FROM Job j LEFT JOIN j.applications a WHERE j.id = :jobId GROUP BY j")
    Object findJobWithApplicationCount(@Param("jobId") Long jobId);

    // Fetch all active jobs with application counts
    @Query("SELECT j, COUNT(a) as applicationCount FROM Job j LEFT JOIN j.applications a WHERE j.isActive = true GROUP BY j ORDER BY j.createdAt DESC")
    List<Object[]> findAllActiveJobsWithApplicationCount();
}