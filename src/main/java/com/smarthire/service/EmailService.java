package com.smarthire.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendVerificationEmail(String to, String token) {
        String verificationUrl = "http://localhost:8080/api/auth/verify?token=" + token;

        // ✅ PRINT TO CONSOLE FOR DEBUGGING
        System.out.println("\n========================================");
        System.out.println("📧 SENDING VERIFICATION EMAIL");
        System.out.println("========================================");
        System.out.println("To: " + to);
        System.out.println("Subject: SmartHire - Verify Your Email");
        System.out.println("Verification Link: " + verificationUrl);
        System.out.println("========================================\n");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("SmartHire - Verify Your Email");
        message.setText("Welcome to SmartHire!\n\n" +
                "Please click the link below to verify your email address:\n" +
                verificationUrl + "\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "Best regards,\nSmartHire Team");

        try {
            mailSender.send(message);
            System.out.println("✅ Email sent successfully to: " + to);
        } catch (Exception e) {
            System.err.println("❌ Failed to send email to: " + to);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String name) {
        System.out.println("\n========================================");
        System.out.println("📧 SENDING WELCOME EMAIL");
        System.out.println("========================================");
        System.out.println("To: " + to);
        System.out.println("========================================\n");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Welcome to SmartHire!");
        message.setText("Hi " + name + ",\n\n" +
                "Thank you for joining SmartHire! We're excited to help you find your dream job.\n\n" +
                "Get started by uploading your resume and exploring job matches.\n\n" +
                "Best regards,\nSmartHire Team");

        try {
            mailSender.send(message);
            System.out.println("✅ Welcome email sent to: " + to);
        } catch (Exception e) {
            System.err.println("❌ Failed to send welcome email: " + e.getMessage());
        }
    }
}