
package com.smarthire.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class InterviewEvaluationResponseDTO {
    private Long evaluationId;
    private BigDecimal score;
    private String sentiment;
    private BigDecimal relevanceScore;
    private BigDecimal completenessScore;
    private BigDecimal fluencyScore;
    private List<String> feedback;
    private Long applicationId;
}