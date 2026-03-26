package com.smarthire.controller;

import com.smarthire.dto.LoginRequest;
import com.smarthire.dto.SignupRequest;
import com.smarthire.model.User;
import com.smarthire.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
            response.put("message", "User registered successfully. Please check your email for verification.");
            response.put("email", registeredUser.getEmail());
            response.put("name", registeredUser.getName());
            response.put("userType", registeredUser.getUserType().name());
            response.put("isRecruiter", registeredUser.getUserType().name().equals("RECRUITER"));
            response.put("isCandidate", registeredUser.getUserType().name().equals("CANDIDATE"));
            response.put("isVerified", registeredUser.isVerified());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest request) {
        try {
            Map<String, Object> authData = userService.authenticateUserWithDetails(request);
            return ResponseEntity.ok(authData);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(401).body(error);
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            String message = userService.verifyEmail(token);
            Map<String, String> response = new HashMap<>();
            response.put("message", message);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}