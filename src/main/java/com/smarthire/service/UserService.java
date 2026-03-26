package com.smarthire.service;

import com.smarthire.config.JwtUtil;
import com.smarthire.dto.LoginRequest;
import com.smarthire.dto.SignupRequest;
import com.smarthire.model.User;
import com.smarthire.model.UserType;
import com.smarthire.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    public Long getUserIdByEmail(String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
            return user.getId();
        } catch (UsernameNotFoundException e) {
            throw new RuntimeException("User not found: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Error fetching user ID: " + e.getMessage());
        }
    }

    public User getUserById(Long id) {
        try {
            return userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        } catch (Exception e) {
            throw new RuntimeException("Error fetching user: " + e.getMessage());
        }
    }

    public User getUserByEmail(String email) {
        try {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        } catch (Exception e) {
            throw new RuntimeException("Error fetching user: " + e.getMessage());
        }
    }

    public User registerUser(SignupRequest request) {
        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                throw new IllegalArgumentException("Email is required");
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("Password is required");
            }
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Name is required");
            }
            if (request.getUserType() == null) {
                throw new IllegalArgumentException("User type is required");
            }

            // Check if email already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already registered. Please use a different email or login.");
            }

            // Create new user
            User user = new User();
            user.setEmail(request.getEmail().toLowerCase().trim());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setName(request.getName().trim());
            user.setUserType(request.getUserType());
            user.setVerified(false);
            user.setProfileCompleted(false);

            // Generate verification token
            String verificationToken = UUID.randomUUID().toString();
            user.setVerificationToken(verificationToken);

            // Save user
            User savedUser = userRepository.save(user);

            // Send verification email
            try {
                emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken, savedUser.getName());
            } catch (Exception e) {
                // Log error but don't fail registration
                System.err.println("Failed to send verification email: " + e.getMessage());
            }

            return savedUser;

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Validation error: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    public String authenticateUser(LoginRequest request) {
        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                throw new IllegalArgumentException("Email is required");
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("Password is required");
            }

            // Check if user exists
            User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + request.getEmail()));

            // Check if email is verified
            if (!user.isVerified()) {
                throw new DisabledException("Email not verified. Please check your inbox and verify your email address before logging in.");
            }

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return jwtUtil.generateToken(userDetails);

        } catch (UsernameNotFoundException e) {
            throw new RuntimeException("Invalid credentials. Please check your email and password.");
        } catch (DisabledException e) {
            throw new RuntimeException(e.getMessage());
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid credentials. Please check your email and password.");
        } catch (AuthenticationException e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Validation error: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    public Map<String, Object> authenticateUserWithDetails(LoginRequest request) {
        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                throw new IllegalArgumentException("Email is required");
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("Password is required");
            }

            // Check if user exists
            User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Check if email is verified
            if (!user.isVerified()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "EMAIL_NOT_VERIFIED");
                errorResponse.put("message", "Please verify your email before logging in. Check your inbox for verification link.");
                errorResponse.put("email", user.getEmail());
                errorResponse.put("isVerified", false);
                throw new RuntimeException(errorResponse.toString());
            }

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("message", "Login successful");
            response.put("email", user.getEmail());
            response.put("name", user.getName());
            response.put("userType", user.getUserType().name());
            response.put("isVerified", user.isVerified());
            response.put("profileCompleted", user.isProfileCompleted());
            response.put("isRecruiter", user.getUserType() == UserType.RECRUITER);
            response.put("isCandidate", user.getUserType() == UserType.CANDIDATE);

            return response;

        } catch (UsernameNotFoundException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "USER_NOT_FOUND");
            error.put("message", "No account found with this email. Please sign up first.");
            throw new RuntimeException(error.toString());
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "VALIDATION_ERROR");
            error.put("message", e.getMessage());
            throw new RuntimeException(error.toString());
        } catch (BadCredentialsException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "INVALID_CREDENTIALS");
            error.put("message", "Invalid password. Please try again.");
            throw new RuntimeException(error.toString());
        } catch (AuthenticationException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "AUTHENTICATION_FAILED");
            error.put("message", "Authentication failed. Please try again.");
            throw new RuntimeException(error.toString());
        } catch (Exception e) {
            if (e.getMessage().contains("EMAIL_NOT_VERIFIED")) {
                throw e;
            }
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "LOGIN_FAILED");
            error.put("message", "Login failed: " + e.getMessage());
            throw new RuntimeException(error.toString());
        }
    }

    public String verifyEmail(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new IllegalArgumentException("Verification token is required");
            }

            User user = userRepository.findByVerificationToken(token)
                    .orElseThrow(() -> new RuntimeException("Invalid or expired verification token"));

            if (user.isVerified()) {
                return "Email already verified. You can now login.";
            }

            user.setVerified(true);
            user.setVerificationToken(null);
            userRepository.save(user);

            return "Email verified successfully! You can now login to your account.";

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Validation error: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Email verification failed: " + e.getMessage());
        }
    }

    // Additional helper method to check if user can login
    public boolean canLogin(String email) {
        try {
            User user = userRepository.findByEmail(email.toLowerCase().trim())
                    .orElse(null);
            return user != null && user.isVerified();
        } catch (Exception e) {
            return false;
        }
    }

    // Method to resend verification email
    public void resendVerificationEmail(String email) {
        try {
            User user = userRepository.findByEmail(email.toLowerCase().trim())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.isVerified()) {
                throw new RuntimeException("Email already verified. You can login directly.");
            }

            // Generate new verification token
            String newToken = UUID.randomUUID().toString();
            user.setVerificationToken(newToken);
            userRepository.save(user);

            // Send new verification email
            emailService.sendVerificationEmail(user.getEmail(), newToken, user.getName());

        } catch (Exception e) {
            throw new RuntimeException("Failed to resend verification email: " + e.getMessage());
        }
    }
}