package com.smarthire.service;

import com.smarthire.model.Resume;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillGapService {

    private final GeminiService geminiService;

    public String generateLearningRoadmap(List<String> targetSkills, Resume resume) {
        return geminiService.generateLearningRoadmap(resume.getSkills(), targetSkills);
    }
}