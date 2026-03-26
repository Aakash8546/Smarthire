package com.smarthire.controller;

import com.smarthire.model.User;
import com.smarthire.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            String email = authentication.getName();
            User user = userService.getUserByEmail(email);

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("name", user.getName());
            response.put("email", user.getEmail());
            response.put("userType", user.getUserType().name());
            response.put("isVerified", user.isVerified());
            response.put("profileCompleted", user.isProfileCompleted());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/type")
    public ResponseEntity<?> getUserType(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            String email = authentication.getName();
            User user = userService.getUserByEmail(email);

            Map<String, Object> response = new HashMap<>();
            response.put("userType", user.getUserType().name());
            response.put("isRecruiter", user.getUserType().name().equals("RECRUITER"));
            response.put("isCandidate", user.getUserType().name().equals("CANDIDATE"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}