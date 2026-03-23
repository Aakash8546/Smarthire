package com.smarthire.service;

import com.smarthire.dto.JobRecommendationResponse;
import com.smarthire.model.*;
import com.smarthire.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private AIService aiService;

    public List<JobRecommendationResponse> getPersonalizedRecommendations(Long userId, int limit) {
        User user = new User();
        user.setId(userId);

        Resume resume = resumeRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Resume not found. Please upload your resume first."));

        List<String> userSkills = resume.getSkillsList();
        List<Job> activeJobs = jobRepository.findActiveJobs();

        List<JobRecommendationResponse> recommendations = new ArrayList<>();

        for (Job job : activeJobs) {
            List<String> jobSkills = job.getRequiredSkillsList();


            long matchingSkills = userSkills.stream()
                    .filter(skill -> jobSkills.stream().anyMatch(jobSkill ->
                            jobSkill.toLowerCase().contains(skill.toLowerCase()) ||
                                    skill.toLowerCase().contains(jobSkill.toLowerCase())))
                    .count();

            int matchPercentage = jobSkills.isEmpty() ? 0 :
                    (int) ((matchingSkills * 100) / jobSkills.size());


            int aiScore = aiService.getJobRecommendationScore(userSkills, jobSkills);


            int finalScore = (matchPercentage * 70 + aiScore * 30) / 100;

            JobRecommendationResponse response = new JobRecommendationResponse();
            response.setJobId(job.getId());
            response.setTitle(job.getTitle());
            response.setCompany(job.getCompany());
            response.setLocation(job.getLocation());
            response.setMatchPercentage(finalScore);
            response.setRequiredSkills(jobSkills);
            response.setUserSkills(userSkills);
            response.setMissingSkillCount(jobSkills.size() - (int) matchingSkills);
            response.setSalaryRange(job.getSalaryRange());

            recommendations.add(response);
        }


        return recommendations.stream()
                .sorted(Comparator.comparing(JobRecommendationResponse::getMatchPercentage).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public SkillGapRoadmap generateSkillGapRoadmap(Long userId, Long jobId) {
        User user = new User();
        user.setId(userId);

        Resume resume = resumeRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        List<String> userSkills = resume.getSkillsList();
        List<String> jobSkills = job.getRequiredSkillsList();

        List<String> missingSkills = jobSkills.stream()
                .filter(jobSkill -> userSkills.stream().noneMatch(userSkill ->
                        userSkill.toLowerCase().contains(jobSkill.toLowerCase()) ||
                                jobSkill.toLowerCase().contains(userSkill.toLowerCase())))
                .collect(Collectors.toList());


        String roadmap = aiService.generateSkillGapRoadmap(missingSkills, job.getTitle());
        int estimatedWeeks = calculateEstimatedWeeks(missingSkills);

        SkillGapRoadmap skillGapRoadmap = new SkillGapRoadmap();
        skillGapRoadmap.setUser(user);
        skillGapRoadmap.setJob(job);
        skillGapRoadmap.setRoadmap(roadmap);
        skillGapRoadmap.setEstimatedWeeks(estimatedWeeks);

        return skillGapRoadmap;
    }

    private int calculateEstimatedWeeks(List<String> missingSkills) {
        if (missingSkills.isEmpty()) return 0;

        int weeks = missingSkills.size();
        return Math.min(8, Math.max(2, weeks));
    }
}