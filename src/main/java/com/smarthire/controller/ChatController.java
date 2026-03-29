// backend/src/main/java/com/smarthire/controller/ChatController.java
package com.smarthire.controller;

import com.smarthire.entity.ChatRoom;
import com.smarthire.entity.Message;
import com.smarthire.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {
    @Autowired
    private ChatService chatService;

    @PostMapping("/room")
    public ResponseEntity<ChatRoom> createRoom(
            @RequestParam Long jobId,
            @RequestParam Long candidateId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long recruiterId = Long.parseLong(userDetails.getUsername()); // Simplified
        return ResponseEntity.ok(chatService.createChatRoom(jobId, candidateId, recruiterId));
    }

    @GetMapping("/room/{id}")
    public ResponseEntity<ChatRoom> getRoom(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.getChatRoom(id));
    }

    @GetMapping("/room/{id}/messages")
    public ResponseEntity<List<Message>> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(chatService.getMessages(id, page, size));
    }

    @PostMapping("/room/{id}/mark-spam/{messageId}")
    public ResponseEntity<Void> markSpam(
            @PathVariable Long id,
            @PathVariable Long messageId) {
        chatService.markSpamMessage(messageId);
        return ResponseEntity.ok().build();
    }
}