package com.smarthire.dto;

import java.util.List;

public class JobRecommendationResponse {
    private Long jobId;
    private String title;
    private String company;
    private String location;
    private Integer matchPercentage;
    private List<String> requiredSkills;
    private List<String> userSkills;
    private Integer missingSkillCount;
    private String salaryRange;

    // Constructors
    public JobRecommendationResponse() {}

    // Getters and Setters
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getMatchPercentage() { return matchPercentage; }
    public void setMatchPercentage(Integer matchPercentage) { this.matchPercentage = matchPercentage; }

    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }

    public List<String> getUserSkills() { return userSkills; }
    public void setUserSkills(List<String> userSkills) { this.userSkills = userSkills; }

    public Integer getMissingSkillCount() { return missingSkillCount; }
    public void setMissingSkillCount(Integer missingSkillCount) { this.missingSkillCount = missingSkillCount; }

    public String getSalaryRange() { return salaryRange; }
    public void setSalaryRange(String salaryRange) { this.salaryRange = salaryRange; }
}