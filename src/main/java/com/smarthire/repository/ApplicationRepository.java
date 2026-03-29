
package com.smarthire.repository;

import com.smarthire.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByJobIdAndUserId(Long jobId, Long userId);

    Page<Application> findByJobId(Long jobId, Pageable pageable);

    Page<Application> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.job.id = :jobId ORDER BY a.score DESC NULLS LAST")
    List<Application> findRankedByJobId(@Param("jobId") Long jobId);

    @Query("SELECT a FROM Application a WHERE a.job.id = :jobId AND a.score IS NOT NULL ORDER BY a.score DESC")
    Page<Application> findRankedByJobIdWithScore(@Param("jobId") Long jobId, Pageable pageable);

    @Query("SELECT AVG(a.score) FROM Application a WHERE a.job.id = :jobId AND a.score IS NOT NULL")
    BigDecimal getAverageScoreForJob(@Param("jobId") Long jobId);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.job.id = :jobId AND a.status = :status")
    long countByJobIdAndStatus(@Param("jobId") Long jobId, @Param("status") String status);

    @Modifying
    @Transactional
    @Query("UPDATE Application a SET a.score = :score, a.status = :status, a.updatedAt = :updatedAt WHERE a.id = :id")
    int updateScoreAndStatus(@Param("id") Long id, @Param("score") BigDecimal score,
                             @Param("status") String status, @Param("updatedAt") LocalDateTime updatedAt);

    List<Application> findByScoreIsNullAndCreatedAtBefore(LocalDateTime before);

    @Query("SELECT a.user.id FROM Application a WHERE a.job.id = :jobId")
    List<Long> findUserIdsByJobId(@Param("jobId") Long jobId);
}