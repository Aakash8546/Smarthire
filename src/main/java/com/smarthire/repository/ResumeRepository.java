
package com.smarthire.repository;

import com.smarthire.entity.Resume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByUserId(Long userId);

    @Query("SELECT r FROM Resume r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<Resume> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM Resume r WHERE r.user.id = :userId AND r.parsedText IS NOT NULL")
    Optional<Resume> findWithTextByUserId(@Param("userId") Long userId);

    @Query("SELECT r.skills FROM Resume r WHERE r.user.id = :userId")
    Optional<String[]> findSkillsByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(r.experienceYears) FROM Resume r WHERE r.user.id IN :userIds")
    Double findAverageExperience(@Param("userIds") List<Long> userIds);

    Page<Resume> findBySkillsContaining(String skill, Pageable pageable);

    @Query("SELECT r FROM Resume r WHERE r.parsedText LIKE %:keyword%")
    Page<Resume> searchResumes(@Param("keyword") String keyword, Pageable pageable);
}