package com.smarthire.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class SignupRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;



    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "User type is required")
    private String userType;
}