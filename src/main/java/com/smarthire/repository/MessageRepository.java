// backend/src/main/java/com/smarthire/repository/MessageRepository.java (Enhanced)
package com.smarthire.repository;

import com.smarthire.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);


    List<Message> findByChatRoomIdAndCreatedAtAfter(Long chatRoomId, LocalDateTime since);

    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :roomId AND m.isSpam = false ORDER BY m.createdAt DESC")
    Page<Message> findNonSpamMessages(@Param("roomId") Long roomId, Pageable pageable);


    @Query("SELECT m FROM Message m WHERE m.id > :lastReadId AND m.chatRoom.id = :roomId AND m.isSpam = false")
    List<Message> findByIdGreaterThanAndChatRoomId(@Param("lastReadId") Long lastReadId, @Param("roomId") Long roomId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.sender.id = :senderId AND m.createdAt > :since")
    long countMessagesFromUserSince(@Param("senderId") Long senderId, @Param("since") LocalDateTime since);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isSpam = true, m.isBlocked = true WHERE m.id = :messageId")
    void blockMessage(@Param("messageId") Long messageId);

    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :roomId AND m.createdAt > :lastRead")
    List<Message> findUnreadMessages(@Param("roomId") Long roomId, @Param("lastRead") LocalDateTime lastRead);


    @Modifying
    @Transactional
    @Query("DELETE FROM Message m WHERE m.createdAt < :olderThan")
    int deleteOldMessages(@Param("olderThan") LocalDateTime olderThan);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.id = :roomId AND m.isSpam = true")
    long countSpamMessagesInRoom(@Param("roomId") Long roomId);
}