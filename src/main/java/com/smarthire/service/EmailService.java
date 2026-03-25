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

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    @Async
    public void sendVerificationEmail(String to, String token, String name) {
        String verificationUrl = baseUrl + "/api/auth/verify?token=" + token;

        String subject = "✨ Verify Your Email - Complete Your SmartHire Journey!";
        String content = String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<meta charset='UTF-8'>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "<title>Verify Email - SmartHire</title>" +
                        "<style>" +
                        "body {" +
                        "  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;" +
                        "  line-height: 1.6;" +
                        "  color: #1a1a1a;" +
                        "  background-color: #f4f7fc;" +
                        "  margin: 0;" +
                        "  padding: 0;" +
                        "}" +
                        ".email-wrapper {" +
                        "  max-width: 600px;" +
                        "  margin: 40px auto;" +
                        "  background: #ffffff;" +
                        "  border-radius: 20px;" +
                        "  overflow: hidden;" +
                        "  box-shadow: 0 20px 40px rgba(0,0,0,0.08);" +
                        "}" +
                        ".header {" +
                        "  background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);" +
                        "  padding: 40px 30px;" +
                        "  text-align: center;" +
                        "}" +
                        ".logo {" +
                        "  font-size: 48px;" +
                        "  margin-bottom: 10px;" +
                        "}" +
                        ".header h1 {" +
                        "  color: #ffffff;" +
                        "  margin: 0;" +
                        "  font-size: 28px;" +
                        "  font-weight: 600;" +
                        "}" +
                        ".content {" +
                        "  padding: 40px 30px;" +
                        "}" +
                        "h2 {" +
                        "  color: #667eea;" +
                        "  font-size: 24px;" +
                        "  margin-bottom: 20px;" +
                        "}" +
                        ".btn {" +
                        "  display: inline-block;" +
                        "  background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);" +
                        "  color: white;" +
                        "  padding: 14px 35px;" +
                        "  text-decoration: none;" +
                        "  border-radius: 50px;" +
                        "  margin: 25px 0;" +
                        "  font-weight: 600;" +
                        "  font-size: 16px;" +
                        "  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);" +
                        "  transition: all 0.3s ease;" +
                        "}" +
                        ".btn:hover {" +
                        "  transform: translateY(-2px);" +
                        "  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);" +
                        "}" +
                        ".info-box {" +
                        "  background: #f8f9ff;" +
                        "  border-left: 4px solid #667eea;" +
                        "  padding: 20px;" +
                        "  border-radius: 12px;" +
                        "  margin: 25px 0;" +
                        "}" +
                        ".verification-link {" +
                        "  background: #f5f5f5;" +
                        "  padding: 15px;" +
                        "  border-radius: 12px;" +
                        "  word-break: break-all;" +
                        "  font-size: 12px;" +
                        "  color: #666;" +
                        "  margin: 20px 0;" +
                        "  font-family: monospace;" +
                        "}" +
                        ".expiry {" +
                        "  background: #fff5f5;" +
                        "  border-left: 4px solid #ff6b6b;" +
                        "  padding: 15px;" +
                        "  border-radius: 12px;" +
                        "  margin: 20px 0;" +
                        "  font-size: 14px;" +
                        "  color: #ff6b6b;" +
                        "}" +
                        ".footer {" +
                        "  text-align: center;" +
                        "  padding: 30px;" +
                        "  background: #f8f9fa;" +
                        "  border-top: 1px solid #e9ecef;" +
                        "  font-size: 13px;" +
                        "  color: #6c757d;" +
                        "}" +
                        ".social-links {" +
                        "  margin: 20px 0;" +
                        "}" +
                        ".social-links a {" +
                        "  color: #667eea;" +
                        "  text-decoration: none;" +
                        "  margin: 0 10px;" +
                        "}" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='email-wrapper'>" +
                        "<div class='header'>" +
                        "<div class='logo'>🎯✨</div>" +
                        "<h1>SmartHire</h1>" +
                        "</div>" +
                        "<div class='content'>" +
                        "<h2>Welcome to SmartHire, %s! 👋</h2>" +
                        "<p style='font-size: 16px; color: #4a5568;'>We're absolutely thrilled to have you join our community of forward-thinking professionals! 🚀</p>" +
                        "<div class='info-box'>" +
                        "<strong>✨ One last step to unlock your career potential:</strong><br/>" +
                        "Verify your email address to access AI-powered job matching, real-time alerts, and personalized career insights." +
                        "</div>" +
                        "<div style='text-align: center;'>" +
                        "<a href='%s' class='btn'>✅ Verify My Account</a>" +
                        "</div>" +
                        "<div class='verification-link'>" +
                        "<strong>🔗 Or copy this link:</strong><br/>%s" +
                        "</div>" +
                        "<div class='expiry'>" +
                        "⏰ <strong>Time-sensitive:</strong> This verification link expires in 24 hours for your security." +
                        "</div>" +
                        "</div>" +
                        "<div class='footer'>" +
                        "<p><strong>Why verify? Here's what awaits you:</strong></p>" +
                        "<p>🎯 AI-powered job matching &nbsp;|&nbsp; 📊 Application tracking &nbsp;|&nbsp; 💡 Skill insights</p>" +
                        "<div class='social-links'>" +
                        "<a href='#'>Twitter</a> • " +
                        "<a href='#'>LinkedIn</a> • " +
                        "<a href='#'>GitHub</a>" +
                        "</div>" +
                        "<p>Need help? Contact us at <a href='mailto:support@smarthire.com' style='color: #667eea;'>support@smarthire.com</a></p>" +
                        "<p style='margin-top: 20px;'>© 2024 SmartHire. All rights reserved.</p>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                name, verificationUrl, verificationUrl
        );

        sendEmail(to, subject, content);
    }

    @Async
    public void sendMatchNotification(String to, String jobTitle, int matchPercentage, String name) {
        String subject = "🎯 " + matchPercentage + "% Match - " + jobTitle + " at SmartHire";
        String content = String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<meta charset='UTF-8'>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "<title>New Job Match - SmartHire</title>" +
                        "<style>" +
                        "body {" +
                        "  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;" +
                        "  line-height: 1.6;" +
                        "  color: #1a1a1a;" +
                        "  background-color: #f4f7fc;" +
                        "  margin: 0;" +
                        "  padding: 0;" +
                        "}" +
                        ".email-wrapper {" +
                        "  max-width: 600px;" +
                        "  margin: 40px auto;" +
                        "  background: #ffffff;" +
                        "  border-radius: 20px;" +
                        "  overflow: hidden;" +
                        "  box-shadow: 0 20px 40px rgba(0,0,0,0.08);" +
                        "}" +
                        ".header {" +
                        "  background: linear-gradient(135deg, #4CAF50 0%%, #45a049 100%%);" +
                        "  padding: 40px 30px;" +
                        "  text-align: center;" +
                        "}" +
                        ".logo {" +
                        "  font-size: 48px;" +
                        "  margin-bottom: 10px;" +
                        "}" +
                        ".header h1 {" +
                        "  color: #ffffff;" +
                        "  margin: 0;" +
                        "  font-size: 28px;" +
                        "  font-weight: 600;" +
                        "}" +
                        ".content {" +
                        "  padding: 40px 30px;" +
                        "}" +
                        "h2 {" +
                        "  color: #4CAF50;" +
                        "  font-size: 28px;" +
                        "  margin-bottom: 15px;" +
                        "}" +
                        ".match-card {" +
                        "  background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);" +
                        "  padding: 30px;" +
                        "  border-radius: 20px;" +
                        "  margin: 30px 0;" +
                        "  text-align: center;" +
                        "  color: white;" +
                        "  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.3);" +
                        "}" +
                        ".match-card h3 {" +
                        "  font-size: 24px;" +
                        "  margin-bottom: 15px;" +
                        "  color: white;" +
                        "}" +
                        ".match-percentage {" +
                        "  font-size: 64px;" +
                        "  font-weight: bold;" +
                        "  margin: 20px 0;" +
                        "  background: rgba(255,255,255,0.2);" +
                        "  display: inline-block;" +
                        "  padding: 20px 40px;" +
                        "  border-radius: 60px;" +
                        "}" +
                        ".btn {" +
                        "  display: inline-block;" +
                        "  background: #4CAF50;" +
                        "  color: white;" +
                        "  padding: 14px 35px;" +
                        "  text-decoration: none;" +
                        "  border-radius: 50px;" +
                        "  margin: 20px 0;" +
                        "  font-weight: 600;" +
                        "  font-size: 16px;" +
                        "  transition: all 0.3s ease;" +
                        "}" +
                        ".btn:hover {" +
                        "  transform: translateY(-2px);" +
                        "  box-shadow: 0 5px 15px rgba(76, 175, 80, 0.3);" +
                        "}" +
                        ".footer {" +
                        "  text-align: center;" +
                        "  padding: 30px;" +
                        "  background: #f8f9fa;" +
                        "  border-top: 1px solid #e9ecef;" +
                        "  font-size: 13px;" +
                        "  color: #6c757d;" +
                        "}" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='email-wrapper'>" +
                        "<div class='header'>" +
                        "<div class='logo'>🎯✨</div>" +
                        "<h1>SmartHire</h1>" +
                        "</div>" +
                        "<div class='content'>" +
                        "<h2>Congratulations %s! 🎉</h2>" +
                        "<p style='font-size: 16px; color: #4a5568;'>We've found an exciting opportunity that matches your profile perfectly!</p>" +
                        "<div class='match-card'>" +
                        "<h3>%s</h3>" +
                        "<div class='match-percentage'>%d%% Match</div>" +
                        "<p style='margin-top: 20px;'>Based on your skills, experience, and preferences</p>" +
                        "</div>" +
                        "<p><strong>Why this role stands out:</strong></p>" +
                        "<ul style='color: #4a5568;'>" +
                        "<li>✓ Excellent alignment with your skill set</li>" +
                        "<li>✓ Competitive compensation package</li>" +
                        "<li>✓ Career growth opportunities</li>" +
                        "</ul>" +
                        "<div style='text-align: center;'>" +
                        "<a href='%s/jobs' class='btn'>🔍 View Full Job Details</a>" +
                        "</div>" +
                        "<p style='text-align: center; color: #6c757d; font-size: 14px; margin-top: 20px;'>Don't wait - opportunities like this are rare! Apply now to take the next step in your career.</p>" +
                        "</div>" +
                        "<div class='footer'>" +
                        "<p>Best regards,<br/><strong>The SmartHire Team</strong> 🚀</p>" +
                        "<p style='margin-top: 10px;'><small>You're receiving this because you're a SmartHire user. <a href='%s/settings' style='color: #667eea;'>Manage notifications</a></small></p>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                name, jobTitle, matchPercentage, baseUrl, baseUrl
        );

        sendEmail(to, subject, content);
    }

    @Async
    public void sendWelcomeEmail(String to, String name) {
        String subject = "🎉 Welcome to SmartHire, " + name + "! Your Career Journey Begins";
        String content = String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<meta charset='UTF-8'>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "<title>Welcome to SmartHire</title>" +
                        "<style>" +
                        "body {" +
                        "  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;" +
                        "  line-height: 1.6;" +
                        "  color: #1a1a1a;" +
                        "  background-color: #f4f7fc;" +
                        "  margin: 0;" +
                        "  padding: 0;" +
                        "}" +
                        ".email-wrapper {" +
                        "  max-width: 600px;" +
                        "  margin: 40px auto;" +
                        "  background: #ffffff;" +
                        "  border-radius: 20px;" +
                        "  overflow: hidden;" +
                        "  box-shadow: 0 20px 40px rgba(0,0,0,0.08);" +
                        "}" +
                        ".header {" +
                        "  background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);" +
                        "  padding: 40px 30px;" +
                        "  text-align: center;" +
                        "}" +
                        ".logo {" +
                        "  font-size: 48px;" +
                        "  margin-bottom: 10px;" +
                        "}" +
                        ".header h1 {" +
                        "  color: #ffffff;" +
                        "  margin: 0;" +
                        "  font-size: 28px;" +
                        "  font-weight: 600;" +
                        "}" +
                        ".content {" +
                        "  padding: 40px 30px;" +
                        "}" +
                        "h2 {" +
                        "  color: #667eea;" +
                        "  font-size: 28px;" +
                        "  margin-bottom: 20px;" +
                        "}" +
                        ".feature-grid {" +
                        "  display: grid;" +
                        "  grid-template-columns: 1fr 1fr;" +
                        "  gap: 20px;" +
                        "  margin: 30px 0;" +
                        "}" +
                        ".feature-card {" +
                        "  background: #f8f9ff;" +
                        "  padding: 20px;" +
                        "  border-radius: 12px;" +
                        "  text-align: center;" +
                        "}" +
                        ".feature-icon {" +
                        "  font-size: 32px;" +
                        "  margin-bottom: 10px;" +
                        "}" +
                        ".feature-card h3 {" +
                        "  font-size: 16px;" +
                        "  margin: 10px 0;" +
                        "  color: #667eea;" +
                        "}" +
                        ".feature-card p {" +
                        "  font-size: 13px;" +
                        "  color: #6c757d;" +
                        "  margin: 0;" +
                        "}" +
                        ".btn {" +
                        "  display: inline-block;" +
                        "  background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);" +
                        "  color: white;" +
                        "  padding: 14px 35px;" +
                        "  text-decoration: none;" +
                        "  border-radius: 50px;" +
                        "  margin: 25px 0;" +
                        "  font-weight: 600;" +
                        "  font-size: 16px;" +
                        "  transition: all 0.3s ease;" +
                        "}" +
                        ".btn:hover {" +
                        "  transform: translateY(-2px);" +
                        "  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.3);" +
                        "}" +
                        ".footer {" +
                        "  text-align: center;" +
                        "  padding: 30px;" +
                        "  background: #f8f9fa;" +
                        "  border-top: 1px solid #e9ecef;" +
                        "  font-size: 13px;" +
                        "  color: #6c757d;" +
                        "}" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='email-wrapper'>" +
                        "<div class='header'>" +
                        "<div class='logo'>🚀✨</div>" +
                        "<h1>SmartHire</h1>" +
                        "</div>" +
                        "<div class='content'>" +
                        "<h2>Welcome to the Future of Hiring, %s! 👋</h2>" +
                        "<p style='font-size: 16px; color: #4a5568;'>We're excited to have you on board! SmartHire uses cutting-edge AI to help you find your dream job faster and smarter.</p>" +
                        "<div class='feature-grid'>" +
                        "<div class='feature-card'>" +
                        "<div class='feature-icon'>📄</div>" +
                        "<h3>AI Resume Analysis</h3>" +
                        "<p>Get instant insights and improvement suggestions</p>" +
                        "</div>" +
                        "<div class='feature-card'>" +
                        "<div class='feature-icon'>🎯</div>" +
                        "<h3>Smart Matching</h3>" +
                        "<p>Personalized job recommendations</p>" +
                        "</div>" +
                        "<div class='feature-card'>" +
                        "<div class='feature-icon'>📊</div>" +
                        "<h3>Track Applications</h3>" +
                        "<p>Monitor your progress in real-time</p>" +
                        "</div>" +
                        "<div class='feature-card'>" +
                        "<div class='feature-icon'>💡</div>" +
                        "<h3>Skill Insights</h3>" +
                        "<p>Identify skill gaps and growth opportunities</p>" +
                        "</div>" +
                        "</div>" +
                        "<div style='text-align: center;'>" +
                        "<a href='%s/dashboard' class='btn'>🚀 Get Started Now</a>" +
                        "</div>" +
                        "<div style='background: #f0f7ff; padding: 20px; border-radius: 12px; margin: 20px 0; text-align: center;'>" +
                        "<strong>✨ Pro Tip:</strong> Complete your profile to 100%% and get 5x more job matches!" +
                        "</div>" +
                        "</div>" +
                        "<div class='footer'>" +
                        "<p>Best regards,<br/><strong>The SmartHire Team</strong></p>" +
                        "<p style='margin-top: 10px;'><small>Need help? Check out our <a href='%s/help' style='color: #667eea;'>Help Center</a> or contact support</small></p>" +
                        "<p style='margin-top: 10px;'>© 2024 SmartHire. Empowering careers with AI.</p>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                name, baseUrl, baseUrl
        );

        sendEmail(to, subject, content);
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            System.out.println("=== Sending Email ===");
            System.out.println("To: " + to);
            System.out.println("Subject: " + subject);
            System.out.println("From: " + fromEmail);
            System.out.println("Base URL: " + baseUrl);

            Email from = new Email(fromEmail, fromName);
            Email toEmail = new Email(to);
            Content content = new Content("text/html", htmlContent);
            Mail mail = new Mail(from, subject, toEmail, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                System.out.println("✅ Email sent successfully to: " + to);
                System.out.println("✅ Status Code: " + response.getStatusCode());
            } else {
                System.err.println("❌ Failed to send email to: " + to);
                System.err.println("❌ Status Code: " + response.getStatusCode());
                System.err.println("❌ Response Body: " + response.getBody());
            }
        } catch (IOException ex) {
            System.err.println("❌ Error sending email to: " + to);
            System.err.println("❌ Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}