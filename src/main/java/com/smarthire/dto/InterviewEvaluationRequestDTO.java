
package com.smarthire.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class InterviewEvaluationRequestDTO {
    @NotNull
    private Long applicationId;

    @NotBlank
    private String question;

    @NotBlank
    private String answer;
}