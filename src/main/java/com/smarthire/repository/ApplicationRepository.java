
package com.smarthire.repository;

import com.smarthire.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<Application> findByJobIdAndUserId(Long jobId, Long userId);
    Page<Application> findByJobId(Long jobId, Pageable pageable);
    Page<Application> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.job.id = :jobId ORDER BY a.score DESC")
    List<Application> findRankedByJobId(@Param("jobId") Long jobId);
}