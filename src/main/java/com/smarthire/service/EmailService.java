package com.smarthire.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    public void sendVerificationEmail(String to, String token, String name) {
        try {
            String verificationUrl = baseUrl + "/api/auth/verify?token=" + token;

            String subject = "Verify Your Email - SmartHire";
            String text = "Welcome " + name + "!\n\n" +
                    "Please verify your email by clicking:\n" +
                    verificationUrl + "\n\n" +
                    "Link expires in 24 hours.\n\n" +
                    "Best regards,\nSmartHire Team";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            System.out.println("✅ Email sent to: " + to);

        } catch (Exception e) {
            System.err.println("❌ Email failed: " + e.getMessage());
        }
    }

    public void sendMatchNotification(String to, String jobTitle, int matchPercentage, String name) {
        try {
            String subject = "New Job Match - " + matchPercentage + "%";
            String text = "Hello " + name + "!\n\n" +
                    "We found a match for you:\n" +
                    "Job: " + jobTitle + "\n" +
                    "Match Score: " + matchPercentage + "%\n\n" +
                    "View: " + baseUrl + "/jobs\n\n" +
                    "Best regards,\nSmartHire Team";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            System.out.println("✅ Match email sent to: " + to);

        } catch (Exception e) {
            System.err.println("❌ Match email failed: " + e.getMessage());
        }
    }

    public void sendWelcomeEmail(String to, String name) {
        try {
            String subject = "Welcome to SmartHire!";
            String text = "Welcome " + name + "!\n\n" +
                    "Upload your resume to get started.\n\n" +
                    "Dashboard: " + baseUrl + "/dashboard\n\n" +
                    "Best regards,\nSmartHire Team";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            System.out.println("✅ Welcome email sent to: " + to);

        } catch (Exception e) {
            System.err.println("❌ Welcome email failed: " + e.getMessage());
        }
    }

    public void sendApplicationConfirmation(String to, String candidateName, String jobTitle, String company, int matchPercentage) {
        try {
            String subject = "Application Submitted - " + jobTitle;
            String text = "Thank you " + candidateName + "!\n\n" +
                    "Applied for: " + jobTitle + " at " + company + "\n" +
                    "Match Score: " + matchPercentage + "%\n\n" +
                    "Track: " + baseUrl + "/dashboard/applications\n\n" +
                    "Best regards,\nSmartHire Team";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            System.out.println("✅ Application confirmation sent to: " + to);

        } catch (Exception e) {
            System.err.println("❌ Application confirmation failed: " + e.getMessage());
        }
    }

    public void sendNewApplicationNotification(String to, String recruiterName, String jobTitle, String candidateName, int matchPercentage) {
        try {
            String subject = "New Application - " + jobTitle;
            String text = "Hello " + recruiterName + "!\n\n" +
                    "New application for: " + jobTitle + "\n" +
                    "Candidate: " + candidateName + "\n" +
                    "Match Score: " + matchPercentage + "%\n\n" +
                    "Review: " + baseUrl + "/recruiter/applications\n\n" +
                    "Best regards,\nSmartHire Team";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            System.out.println("✅ New application notification sent to: " + to);

        } catch (Exception e) {
            System.err.println("❌ New application notification failed: " + e.getMessage());
        }
    }

    public void sendApplicationStatusUpdate(String to, String candidateName, String jobTitle, String company, String status, String notes) {
        try {
            String subject = "Application Status Update - " + jobTitle;
            String text = "Hello " + candidateName + "!\n\n" +
                    "Your application for " + jobTitle + " at " + company + "\n" +
                    "Status: " + status + "\n" +
                    (notes != null ? "Notes: " + notes + "\n\n" : "\n") +
                    "View: " + baseUrl + "/dashboard/applications\n\n" +
                    "Best regards,\nSmartHire Team";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            System.out.println("✅ Status update sent to: " + to);

        } catch (Exception e) {
            System.err.println("❌ Status update failed: " + e.getMessage());
        }
    }
}