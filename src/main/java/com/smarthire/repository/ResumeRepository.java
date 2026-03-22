package com.smarthire.repository;

import com.smarthire.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, String> {
    List<Resume> findByUserId(String userId);
    Optional<Resume> findTopByUserIdOrderByCreatedAtDesc(String userId);
}