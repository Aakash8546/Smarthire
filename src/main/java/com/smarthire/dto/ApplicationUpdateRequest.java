package com.smarthire.dto;

import com.smarthire.model.ApplicationStatus;
import lombok.Data;

@Data
public class ApplicationUpdateRequest {
    private ApplicationStatus status;
    private String notes;
}