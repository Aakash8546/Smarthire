package com.smarthire.service;

import com.smarthire.dto.MatchResult;
import com.smarthire.model.Job;
import com.smarthire.model.Resume;
import com.smarthire.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobRecommendationService {

    private final JobRepository jobRepository;
    private final JobMatchingService matchingService;

    public List<MatchResult> getPersonalizedRecommendations(Resume resume) {
        List<Job> allJobs = jobRepository.findAll();
        List<MatchResult> recommendations = new ArrayList<>();

        for (Job job : allJobs) {
            var match = matchingService.matchResumeWithJob(resume, job);

            MatchResult result = new MatchResult();
            result.setJobId(job.getId());
            result.setJobTitle(job.getTitle());
            result.setCompany(job.getCompany());
            result.setMatchPercentage(match.getMatchPercentage());
            result.setMissingSkills(match.getMissingSkills());
            result.setMatchedSkills(match.getMatchedSkills());

            recommendations.add(result);
        }

        recommendations.sort((a, b) -> b.getMatchPercentage().compareTo(a.getMatchPercentage()));
        return recommendations;
    }
}