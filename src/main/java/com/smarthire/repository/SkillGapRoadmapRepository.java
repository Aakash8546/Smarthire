package com.smarthire.repository;

import com.smarthire.model.SkillGapRoadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillGapRoadmapRepository extends JpaRepository<SkillGapRoadmap, Long> {

    Optional<SkillGapRoadmap> findByUserIdAndJobId(Long userId, Long jobId);

    List<SkillGapRoadmap> findByUserId(Long userId);

    List<SkillGapRoadmap> findByJobId(Long jobId);

    void deleteByUserId(Long userId);
}