package com.smarthire.repository;

import com.smarthire.model.Resume;
import com.smarthire.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByUser(User user);

    Optional<Resume> findByUserId(Long userId);  // Keep this for convenience

    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);
}