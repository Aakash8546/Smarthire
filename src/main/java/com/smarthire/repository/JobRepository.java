
package com.smarthire.repository;

import com.smarthire.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {


    List<Job> findByStatus(String status);

    Page<Job> findByStatus(String status, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.status = 'ACTIVE' ORDER BY j.createdAt DESC")
    List<Job> findTopNByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.status = 'ACTIVE' ORDER BY j.createdAt DESC")
    Page<Job> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.status = :status AND " +
            "(LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Job> searchJobs(@Param("status") String status, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.recruiter.id = :recruiterId")
    Page<Job> findByRecruiterId(@Param("recruiterId") Long recruiterId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.job.id = :jobId")
    long countApplicationsByJobId(@Param("jobId") Long jobId);

    List<Job> findByCreatedAtAfter(LocalDateTime since);
}