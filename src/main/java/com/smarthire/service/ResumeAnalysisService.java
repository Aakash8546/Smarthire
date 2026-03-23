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
import java.util.UUID;

@Service
public class ResumeAnalysisService {

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AIService aiService;

    @Autowired
    private ResumeTextExtractor textExtractor;

    private final String UPLOAD_DIR = "uploads/resumes/";

    public ResumeAnalysisResponse analyzeResume(Long userId, MultipartFile file) throws IOException {
        System.out.println("=== Analyzing Resume for User ID: " + userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    System.err.println("User not found with ID: " + userId);
                    return new RuntimeException("User not found");
                });

        System.out.println("User found: " + user.getEmail());


        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("Created upload directory: " + UPLOAD_DIR);
        }


        String fileName = file.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.write(filePath, file.getBytes());
        System.out.println("File saved: " + filePath);


        String resumeText;
        try {
            resumeText = textExtractor.extractText(file);
            System.out.println("Extracted text length: " + resumeText.length());
            System.out.println("First 200 chars: " + resumeText.substring(0, Math.min(200, resumeText.length())));
        } catch (Exception e) {
            System.err.println("Failed to extract text: " + e.getMessage());
            throw new IOException("Failed to extract text from resume: " + e.getMessage(), e);
        }


        System.out.println("Calling AI service for analysis...");
        ResumeAnalysisResponse analysis = aiService.analyzeResume(resumeText);
        System.out.println("AI Analysis completed. Score: " + analysis.getScore());
        System.out.println("Skills extracted: " + analysis.getExtractedSkills());


        Resume resume = resumeRepository.findByUser(user).orElse(new Resume());
        resume.setUser(user);
        resume.setContent(resumeText.substring(0, Math.min(resumeText.length(), 10000)));
        resume.setFilePath(filePath.toString());
        resume.setFileName(fileName);
        resume.setExtractedSkills(String.join(",", analysis.getExtractedSkills()));
        resume.setAiScore(analysis.getScore());
        resume.setSkillSuggestions(String.join("||", analysis.getSuggestions()));
        resume.setAnalysisDate(LocalDateTime.now());

        resumeRepository.save(resume);
        System.out.println("Resume saved to database with ID: " + resume.getId());

        analysis.setResumeId(resume.getId());
        analysis.setMessage("Resume analyzed successfully!");

        return analysis;
    }

    public ResumeAnalysisResponse getResumeAnalysis(Long userId) {
        System.out.println("=== Getting Resume Analysis for User ID: " + userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        System.out.println("User found: " + user.getEmail());

        Resume resume = resumeRepository.findByUser(user)
                .orElseThrow(() -> {
                    System.err.println("No resume found for user: " + user.getEmail());
                    return new RuntimeException("No resume found. Please upload your resume first.");
                });

        System.out.println("Resume found with ID: " + resume.getId());
        System.out.println("AI Score: " + resume.getAiScore());
        System.out.println("Skills: " + resume.getSkillsList());

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
        if (user == null) {
            System.err.println("User not found with ID: " + userId);
            return false;
        }
        boolean exists = resumeRepository.existsByUser(user);
        System.out.println("Resume exists for user " + user.getEmail() + ": " + exists);
        return exists;
    }
}