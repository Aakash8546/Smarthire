package com.smarthire.model;

public enum ApplicationStatus {
    PENDING("Pending Review"),
    REVIEWING("Under Review"),
    SHORTLISTED("Shortlisted"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected"),
    INTERVIEW_SCHEDULED("Interview Scheduled");

    private final String displayName;

    ApplicationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}