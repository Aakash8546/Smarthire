
package com.smarthire.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FraudCheckResponseDTO {
    private boolean isFraud;
    private BigDecimal confidence;
    private String reason;
    private String[] redFlags;
    private Long resumeId;
}