package com.smarthire.controller;

import com.smarthire.dto.LoginRequest;
import com.smarthire.dto.SignupRequest;
import com.smarthire.model.User;
import com.smarthire.model.UserType;
import com.smarthire.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest request) {
        try {
            User registeredUser = userService.registerUser(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully. Please check your email for verification.");
            response.put("email", registeredUser.getEmail());
            response.put("name", registeredUser.getName());
            response.put("userType", registeredUser.getUserType());
            response.put("isRecruiter", registeredUser.getUserType() == UserType.RECRUITER);
            response.put("isCandidate", registeredUser.getUserType() == UserType.CANDIDATE);
            response.put("isVerified", registeredUser.isVerified());
            response.put("requiresVerification", true);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "REGISTRATION_FAILED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest request) {
        try {
            Map<String, Object> authData = userService.authenticateUserWithDetails(request);
            return ResponseEntity.ok(authData);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);

            String errorMessage = e.getMessage();

            if (errorMessage.contains("EMAIL_NOT_VERIFIED")) {
                error.put("error", "EMAIL_NOT_VERIFIED");
                error.put("message", "Please verify your email before logging in.");
                error.put("requiresVerification", true);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            } else if (errorMessage.contains("USER_NOT_FOUND")) {
                error.put("error", "USER_NOT_FOUND");
                error.put("message", "No account found with this email. Please sign up first.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            } else if (errorMessage.contains("INVALID_CREDENTIALS")) {
                error.put("error", "INVALID_CREDENTIALS");
                error.put("message", "Invalid email or password. Please try again.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            } else if (errorMessage.contains("VALIDATION_ERROR")) {
                error.put("error", "VALIDATION_ERROR");
                error.put("message", errorMessage);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            } else {
                error.put("error", "LOGIN_FAILED");
                error.put("message", errorMessage);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            String message = userService.verifyEmail(token);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);
            response.put("canLogin", true);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "VERIFICATION_FAILED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestParam String email) {
        try {
            userService.resendVerificationEmail(email);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Verification email sent successfully. Please check your inbox.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "RESEND_FAILED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/check-verification")
    public ResponseEntity<?> checkVerification(@RequestParam String email) {
        try {
            boolean canLogin = userService.canLogin(email);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("email", email);
            response.put("canLogin", canLogin);
            response.put("message", canLogin ? "Email verified. You can login." : "Email not verified. Please verify your email.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "CHECK_FAILED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}