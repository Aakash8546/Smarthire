
package com.smarthire.websocket;

import com.smarthire.dto.MessageDTO;
import com.smarthire.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@Slf4j
public class ChatWebSocketHandler {

    @Autowired
    private ChatService chatService;

    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/messages/{roomId}")
    public MessageDTO sendMessage(
            @DestinationVariable Long roomId,
            @Payload MessageDTO message,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {

        log.info("WebSocket message received for room: {} from user: {}", roomId, principal.getName());

        // Process message through spam detection
        Long senderId = Long.parseLong(principal.getName()); // Simplified
        var savedMessage = chatService.sendMessage(roomId, senderId, message.getContent());

        message.setId(savedMessage.getId());
        message.setIsSpam(savedMessage.getIsSpam());
        message.setIsBlocked(savedMessage.getIsBlocked());
        message.setTimestamp(savedMessage.getCreatedAt());

        return message;
    }
}