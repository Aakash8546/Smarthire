package com.smarthire.dto;

import com.smarthire.model.ApplicationStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApplicationRequest {
    private Long jobId;
}