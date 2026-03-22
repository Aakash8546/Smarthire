package com.smarthire.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    @Value("${sendgrid.api-key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    @Value("${sendgrid.from-name}")
    private String fromName;

    @Async
    public void sendVerificationEmail(String to, String token, String name) {
        String verificationUrl = "http://localhost:8080/api/auth/verify?token=" + token;

        String subject = "Verify Your Email - SmartHire";
        String content = String.format(
                "<html>" +
                        "<body style='font-family: Arial, sans-serif;'>" +
                        "<h2>Welcome to SmartHire, %s!</h2>" +
                        "<p>Thank you for registering. Please verify your email address by clicking the link below:</p>" +
                        "<p><a href='%s' style='background-color: #4CAF50; color: white; padding: 10px 20px; " +
                        "text-decoration: none; border-radius: 5px;'>Verify Email</a></p>" +
                        "<p>Or copy and paste this link: <br/>%s</p>" +
                        "<p>This link will expire in 24 hours.</p>" +
                        "<br/>" +
                        "<p>Best regards,<br/>SmartHire Team</p>" +
                        "</body>" +
                        "</html>",
                name, verificationUrl, verificationUrl
        );

        sendEmail(to, subject, content);
    }

    @Async
    public void sendMatchNotification(String to, String jobTitle, int matchPercentage, String name) {
        String subject = "🎯 New Job Match Found - SmartHire";
        String content = String.format(
                "<html>" +
                        "<body style='font-family: Arial, sans-serif;'>" +
                        "<h2>Congratulations %s!</h2>" +
                        "<p>We found a great job match for you:</p>" +
                        "<div style='background-color: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                        "<h3>%s</h3>" +
                        "<p><strong>Match Score:</strong> %d%%</p>" +
                        "</div>" +
                        "<p>Login to your SmartHire account to view more details and apply.</p>" +
                        "<p><a href='http://localhost:3000/jobs' style='background-color: #4CAF50; color: white; " +
                        "padding: 10px 20px; text-decoration: none; border-radius: 5px;'>View Job</a></p>" +
                        "<br/>" +
                        "<p>Best regards,<br/>SmartHire Team</p>" +
                        "</body>" +
                        "</html>",
                name, jobTitle, matchPercentage
        );

        sendEmail(to, subject, content);
    }

    @Async
    public void sendWelcomeEmail(String to, String name) {
        String subject = "Welcome to SmartHire!";
        String content = String.format(
                "<html>" +
                        "<body style='font-family: Arial, sans-serif;'>" +
                        "<h2>Welcome to SmartHire, %s!</h2>" +
                        "<p>We're excited to help you find your dream job.</p>" +
                        "<p>Here's what you can do:</p>" +
                        "<ul>" +
                        "<li>Upload your resume for AI analysis</li>" +
                        "<li>Get personalized job recommendations</li>" +
                        "<li>Track your applications</li>" +
                        "<li>Receive skill improvement suggestions</li>" +
                        "</ul>" +
                        "<p><a href='http://localhost:3000/dashboard' style='background-color: #4CAF50; color: white; " +
                        "padding: 10px 20px; text-decoration: none; border-radius: 5px;'>Go to Dashboard</a></p>" +
                        "<br/>" +
                        "<p>Best regards,<br/>SmartHire Team</p>" +
                        "</body>" +
                        "</html>",
                name
        );

        sendEmail(to, subject, content);
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        Email from = new Email(fromEmail, fromName);
        Email toEmail = new Email(to);
        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, toEmail, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                System.out.println("Email sent successfully to: " + to);
            } else {
                System.err.println("Failed to send email. Status code: " + response.getStatusCode());
            }
        } catch (IOException ex) {
            System.err.println("Error sending email: " + ex.getMessage());
        }
    }
}