// backend/src/main/java/com/smarthire/service/ChatService.java
package com.smarthire.service;

import com.smarthire.entity.*;
import com.smarthire.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ChatService {
    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MLServiceClient mlServiceClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CHAT_ROOM_PREFIX = "chat:room:";
    private static final String RECENT_MESSAGES_PREFIX = "chat:messages:";

    @Transactional
    public ChatRoom createChatRoom(Long jobId, Long candidateId, Long recruiterId) {
        ChatRoom chatRoom = new ChatRoom();

        Job job = new Job();
        job.setId(jobId);
        chatRoom.setJob(job);

        User candidate = new User();
        candidate.setId(candidateId);
        chatRoom.setCandidate(candidate);

        User recruiter = new User();
        recruiter.setId(recruiterId);
        chatRoom.setRecruiter(recruiter);

        chatRoom.setStatus("ACTIVE");
        chatRoom.setCreatedAt(LocalDateTime.now());
        chatRoom.setUpdatedAt(LocalDateTime.now());

        ChatRoom saved = chatRoomRepository.save(chatRoom);

        // Cache chat room info
        redisTemplate.opsForHash().put(CHAT_ROOM_PREFIX + saved.getId(), "jobId", jobId);
        redisTemplate.opsForHash().put(CHAT_ROOM_PREFIX + saved.getId(), "candidateId", candidateId);
        redisTemplate.opsForHash().put(CHAT_ROOM_PREFIX + saved.getId(), "recruiterId", recruiterId);
        redisTemplate.expire(CHAT_ROOM_PREFIX + saved.getId(), 24, TimeUnit.HOURS);

        return saved;
    }

    @Transactional
    public Message sendMessage(Long roomId, Long senderId, String content) {
        // Check for spam
        Map<String, Object> spamCheck = mlServiceClient.detectSpam(content);
        boolean isSpam = (boolean) spamCheck.getOrDefault("is_spam", false);
        boolean isBlocked = isSpam && (double) spamCheck.getOrDefault("confidence", 0.0) > 0.8;

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        User sender = new User();
        sender.setId(senderId);

        Message message = new Message();
        message.setChatRoom(chatRoom);
        message.setSender(sender);
        message.setContent(content);
        message.setIsSpam(isSpam);
        message.setIsBlocked(isBlocked);
        message.setCreatedAt(LocalDateTime.now());

        Message saved = messageRepository.save(message);

        // Cache recent messages (keep last 50)
        String cacheKey = RECENT_MESSAGES_PREFIX + roomId;
        redisTemplate.opsForList().leftPush(cacheKey, saved);
        redisTemplate.opsForList().trim(cacheKey, 0, 49);
        redisTemplate.expire(cacheKey, 1, TimeUnit.HOURS);

        return saved;
    }

    public List<Message> getMessages(Long roomId, int page, int size) {
        // Try cache first for recent messages
        String cacheKey = RECENT_MESSAGES_PREFIX + roomId;
        List<Object> cachedMessages = redisTemplate.opsForList().range(cacheKey, 0, -1);

        if (cachedMessages != null && !cachedMessages.isEmpty() && page == 0) {
            List<Message> messages = new ArrayList<>();
            for (Object obj : cachedMessages) {
                if (obj instanceof Message) {
                    messages.add((Message) obj);
                }
            }
            return messages;
        }

        // Fallback to database
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagePage = messageRepository.findByChatRoomIdOrderByCreatedAtDesc(roomId, pageable);
        return messagePage.getContent();
    }

    public void markSpamMessage(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setIsSpam(true);
        messageRepository.save(message);

        // Update cache
        String cacheKey = RECENT_MESSAGES_PREFIX + message.getChatRoom().getId();
        redisTemplate.opsForList().remove(cacheKey, 1, message);
    }
}