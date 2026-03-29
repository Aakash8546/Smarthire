
package com.smarthire.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageDTO {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String senderName;
    private String content;
    private Boolean isSpam;
    private Boolean isBlocked;
    private LocalDateTime timestamp;
}