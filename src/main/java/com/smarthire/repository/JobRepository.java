
package com.smarthire.repository;

import com.smarthire.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    Page<Job> findByStatus(String status, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.status = 'ACTIVE' AND " +
            "(j.title LIKE %:keyword% OR j.description LIKE %:keyword%)")
    Page<Job> searchJobs(String keyword, Pageable pageable);
}