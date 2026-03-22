package com.smarthire.service;

import com.smarthire.dto.SignupRequest;
import com.smarthire.model.User;
import com.smarthire.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ✅ SET TO TRUE FOR TESTING (NO EMAIL)
    private boolean autoVerify = true;

    public User register(SignupRequest request) {
        System.out.println("\n========================================");
        System.out.println("📝 REGISTRATION REQUEST RECEIVED");
        System.out.println("Email: " + request.getEmail());
        System.out.println("========================================\n");

        if (userRepository.existsByEmail(request.getEmail())) {
            System.out.println("❌ Email already exists: " + request.getEmail());
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole(request.getRole());

        if (autoVerify) {
            // ✅ AUTO-VERIFY - NO EMAIL NEEDED
            user.setVerified(true);
            user.setVerificationToken(null);
            System.out.println("✅ USER AUTO-VERIFIED: " + request.getEmail());
        } else {
            // Send verification email
            user.setVerified(false);
            String verificationToken = UUID.randomUUID().toString();
            user.setVerificationToken(verificationToken);
            System.out.println("📧 Sending verification email to: " + request.getEmail());
            emailService.sendVerificationEmail(user.getEmail(), verificationToken);
        }

        User savedUser = userRepository.save(user);
        System.out.println("✅ User saved to database with ID: " + savedUser.getId());
        System.out.println("✅ Verification status: " + (savedUser.isVerified() ? "VERIFIED" : "NOT VERIFIED"));

        return savedUser;
    }

    public User login(String email, String password) {
        System.out.println("\n========================================");
        System.out.println("🔐 LOGIN ATTEMPT");
        System.out.println("Email: " + email);
        System.out.println("========================================\n");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        if (!user.isVerified()) {
            throw new RuntimeException("Email not verified");
        }

        System.out.println("✅ Login successful for: " + email);
        return user;
    }

    public boolean verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        user.setVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        System.out.println("✅ Email verified for: " + user.getEmail());

        return true;
    }
}