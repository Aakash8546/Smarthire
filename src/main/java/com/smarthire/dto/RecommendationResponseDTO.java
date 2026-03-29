
package com.smarthire.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RecommendationResponseDTO {
    private Long jobId;
    private String title;
    private String description;
    private String location;
    private String salaryRange;
    private BigDecimal score;
    private String reason;
}