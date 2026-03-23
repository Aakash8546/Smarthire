package com.smarthire.repository;

import com.smarthire.model.Application;
import com.smarthire.model.Job;
import com.smarthire.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByUser(User user);
    List<Application> findByJob(Job job);
    Optional<Application> findByUserAndJob(User user, Job job);

    @Query("SELECT a FROM Application a WHERE a.job.recruiter = :recruiter ORDER BY a.matchPercentage DESC")
    List<Application> findByJobRecruiterOrderByMatchPercentageDesc(@Param("recruiter") User recruiter);

    boolean existsByUserAndJob(User user, Job job);

