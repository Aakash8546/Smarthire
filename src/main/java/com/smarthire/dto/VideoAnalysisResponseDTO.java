
package com.smarthire.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class VideoAnalysisResponseDTO {
    private Long videoId;
    private BigDecimal overallScore;
    private BigDecimal confidenceScore;
    private BigDecimal eyeContactScore;
    private BigDecimal clarityScore;
    private Map<String, BigDecimal> emotionScores;
    private String feedback;
}