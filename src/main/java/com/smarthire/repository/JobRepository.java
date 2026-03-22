package com.smarthire.repository;

import com.smarthire.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {
    @Query("SELECT j FROM Job j WHERE j.skills LIKE %:skill%")
    List<Job> findBySkillContaining(@Param("skill") String skill);

    List<Job> findAllByOrderByCreatedAtDesc();
}