
package com.smarthire.service;

import com.smarthire.dto.ChatMessageDTO;
import com.smarthire.dto.TypingIndicatorDTO;
import com.smarthire.entity.*;
import com.smarthire.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MLServiceClient mlServiceClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHAT_ROOM_PREFIX = "chat:room:";
    private static final String MESSAGES_PREFIX = "chat:messages:";
    private static final String TYPING_PREFIX = "chat:typing:";
    private static final String RATE_LIMIT_PREFIX = "chat:ratelimit:";
    private static final int MAX_MESSAGES_PER_MINUTE = 20;
    private static final int BLOCK_DURATION_MINUTES = 5;

    @Transactional
    public ChatRoom createChatRoom(Long jobId, Long candidateId, Long recruiterId) {
        // Check if room already exists
        Optional<ChatRoom> existing = chatRoomRepository
                .findByJobIdAndCandidateIdAndRecruiterId(jobId, candidateId, recruiterId);

        if (existing.isPresent()) {
            return existing.get();
        }

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
        cacheChatRoomInfo(saved);

        return saved;
    }

    @Transactional
    public ChatMessageDTO sendMessage(Long roomId, Long senderId, String content) {
        // Rate limiting check
        if (isRateLimited(senderId)) {
            throw new RuntimeException("Rate limit exceeded. Please wait before sending more messages.");
        }

        // Check if sender is blocked
        if (isUserBlocked(senderId)) {
            throw new RuntimeException("You have been temporarily blocked due to spam.");
        }

        // Spam detection
        Map<String, Object> spamCheck = mlServiceClient.detectSpam(content);
        boolean isSpam = (boolean) spamCheck.getOrDefault("is_spam", false);
        boolean isBlocked = isSpam && (double) spamCheck.getOrDefault("confidence", 0.0) > 0.8;

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Message message = new Message();
        message.setChatRoom(chatRoom);
        message.setSender(sender);
        message.setContent(content);
        message.setIsSpam(isSpam);
        message.setIsBlocked(isBlocked);
        message.setCreatedAt(LocalDateTime.now());

        Message saved = messageRepository.save(message);

        // Update rate limiting
        updateRateLimit(senderId);

        // If message is spam, block user
        if (isSpam && (double) spamCheck.getOrDefault("confidence", 0.0) > 0.9) {
            blockUser(senderId);
        }

        // Cache recent messages
        cacheMessage(saved);

        // Convert to DTO
        ChatMessageDTO dto = convertToDTO(saved);

        // Publish to Redis for multi-instance scaling
        publishToRedis("chat:message:" + roomId, dto);

        return dto;
    }

    @Transactional
    public void markMessageAsRead(Long roomId, Long userId, Long messageId) {
        String readReceiptKey = "chat:read:" + roomId + ":" + userId;
        redisTemplate.opsForValue().set(readReceiptKey, messageId.toString(), 24, TimeUnit.HOURS);

        // Notify other participants
        Map<String, Object> receipt = new HashMap<>();
        receipt.put("userId", userId);
        receipt.put("messageId", messageId);
        receipt.put("roomId", roomId);
        receipt.put("timestamp", LocalDateTime.now());

        publishToRedis("chat:read:" + roomId, receipt);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/read", receipt);
    }

    public void sendTypingIndicator(Long roomId, Long userId, boolean isTyping) {
        String typingKey = TYPING_PREFIX + roomId;

        if (isTyping) {
            redisTemplate.opsForHash().put(typingKey, userId.toString(), System.currentTimeMillis());
            redisTemplate.expire(typingKey, 5, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForHash().delete(typingKey, userId.toString());
        }

        TypingIndicatorDTO indicator = new TypingIndicatorDTO();
        indicator.setUserId(userId);
        indicator.setRoomId(roomId);
        indicator.setTyping(isTyping);
        indicator.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/typing", indicator);
    }

    public List<Long> getTypingUsers(Long roomId) {
        String typingKey = TYPING_PREFIX + roomId;
        Map<Object, Object> typingMap = redisTemplate.opsForHash().entries(typingKey);

        List<Long> typingUsers = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Map.Entry<Object, Object> entry : typingMap.entrySet()) {
            long timestamp = Long.parseLong(entry.getValue().toString());
            if (now - timestamp < 5000) { // Typing indicator expires after 5 seconds
                typingUsers.add(Long.parseLong(entry.getKey().toString()));
            }
        }

        return typingUsers;
    }

    public List<ChatMessageDTO> getMessages(Long roomId, int page, int size, Long userId) {
        // Mark messages as read for this user
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagePage = messageRepository.findNonSpamMessages(roomId, pageable);

        List<ChatMessageDTO> messages = messagePage.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());

        // Mark messages as read
        if (!messages.isEmpty()) {
            Long lastMessageId = messages.get(0).getId();
            markMessageAsRead(roomId, userId, lastMessageId);
        }

        return messages;
    }

    public List<ChatMessageDTO> getUnreadMessages(Long roomId, Long userId) {
        String readReceiptKey = "chat:read:" + roomId + ":" + userId;
        String lastReadIdStr = (String) redisTemplate.opsForValue().get(readReceiptKey);

        Long lastReadId = lastReadIdStr != null ? Long.parseLong(lastReadIdStr) : 0;

        List<Message> unreadMessages = messageRepository.findByIdGreaterThanAndChatRoomId(lastReadId, roomId);

        return unreadMessages.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    private boolean isRateLimited(Long userId) {
        String key = RATE_LIMIT_PREFIX + userId + ":" +
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        return count != null && count >= MAX_MESSAGES_PER_MINUTE;
    }

    private void updateRateLimit(Long userId) {
        String key = RATE_LIMIT_PREFIX + userId + ":" +
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 1, TimeUnit.MINUTES);
    }

    private boolean isUserBlocked(Long userId) {
        String blockKey = "chat:blocked:" + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blockKey));
    }

    private void blockUser(Long userId) {
        String blockKey = "chat:blocked:" + userId;
        redisTemplate.opsForValue().set(blockKey, true, BLOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        log.warn("User {} has been blocked for spam", userId);
    }

    private void cacheChatRoomInfo(ChatRoom chatRoom) {
        String key = CHAT_ROOM_PREFIX + chatRoom.getId();
        redisTemplate.opsForHash().put(key, "jobId", chatRoom.getJob().getId());
        redisTemplate.opsForHash().put(key, "candidateId", chatRoom.getCandidate().getId());
        redisTemplate.opsForHash().put(key, "recruiterId", chatRoom.getRecruiter().getId());
        redisTemplate.opsForHash().put(key, "status", chatRoom.getStatus());
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
    }

    private void cacheMessage(Message message) {
        String key = MESSAGES_PREFIX + message.getChatRoom().getId();
        ChatMessageDTO dto = convertToDTO(message);

        redisTemplate.opsForList().leftPush(key, dto);
        redisTemplate.opsForList().trim(key, 0, 99); // Keep last 100 messages
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
    }

    private void publishToRedis(String channel, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(channel, json);
        } catch (Exception e) {
            log.error("Error publishing to Redis", e);
        }
    }

    private ChatMessageDTO convertToDTO(Message message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setRoomId(message.getChatRoom().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getFirstName() + " " + message.getSender().getLastName());
        dto.setContent(message.getContent());
        dto.setIsSpam(message.getIsSpam());
        dto.setIsBlocked(message.getIsBlocked());
        dto.setTimestamp(message.getCreatedAt());
        return dto;
    }
}