package com.smarthire.service;

import com.smarthire.dto.ResumeAnalysisResponse;
import com.smarthire.model.Resume;
import com.smarthire.model.User;
import com.smarthire.repository.ResumeRepository;
import com.smarthire.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ResumeAnalysisService {

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AIService aiService;

    private final String UPLOAD_DIR = "uploads/resumes/";

    public ResumeAnalysisResponse analyzeResume(Long userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create upload directory if not exists
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save file
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        // Extract text from resume (simplified - in production use PDF parser like Apache PDFBox)
        String resumeText = new String(file.getBytes());

        // Call AI for analysis
        ResumeAnalysisResponse analysis = aiService.analyzeResume(resumeText);

        // Save or update resume in database
        Resume resume = resumeRepository.findByUser(user).orElse(new Resume());
        resume.setUser(user);
        resume.setContent(resumeText.substring(0, Math.min(resumeText.length(), 5000))); // Limit content length
        resume.setFilePath(filePath.toString());
        resume.setFileName(file.getOriginalFilename());
        resume.setExtractedSkills(String.join(",", analysis.getExtractedSkills()));
        resume.setAiScore(analysis.getScore());
        resume.setSkillSuggestions(String.join("||", analysis.getSuggestions()));
        resume.setAnalysisDate(LocalDateTime.now());

        resumeRepository.save(resume);

        analysis.setResumeId(resume.getId());
        analysis.setMessage("Resume analyzed successfully!");

        return analysis;
    }

    public ResumeAnalysisResponse getResumeAnalysis(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resume resume = resumeRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("No resume found. Please upload your resume first."));

        ResumeAnalysisResponse response = new ResumeAnalysisResponse();
        response.setResumeId(resume.getId());
        response.setScore(resume.getAiScore());
        response.setExtractedSkills(resume.getSkillsList());
        response.setSuggestions(resume.getSuggestionsList());
        response.setAnalysisDate(resume.getAnalysisDate() != null ? resume.getAnalysisDate().toString() : null);

        return response;
    }

    public boolean hasResume(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && resumeRepository.existsByUser(user);
    }
}