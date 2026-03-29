
package com.smarthire.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TypingIndicatorDTO {
    private Long userId;
    private Long roomId;
    private boolean typing;
    private LocalDateTime timestamp;
}