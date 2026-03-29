
package com.smarthire.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class FraudCheckRequestDTO {
    @NotNull
    private Long resumeId;
    private Long applicationId;
}