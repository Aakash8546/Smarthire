package com.smarthire.repository;

import com.smarthire.model.Resume;
import com.smarthire.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByUser(User user);

    boolean existsByUser(User user);

    // Add this method to find resume by user ID
    Optional<Resume> findByUserId(Long userId);

    // Also add this to check if resume exists by user ID
    boolean existsByUserId(Long userId);
}