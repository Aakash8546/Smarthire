
package com.smarthire.repository;

import com.smarthire.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByJobIdAndCandidateIdAndRecruiterId(
            Long jobId, Long candidateId, Long recruiterId);

    Page<ChatRoom> findByCandidateId(Long candidateId, Pageable pageable);

    Page<ChatRoom> findByRecruiterId(Long recruiterId, Pageable pageable);

    @Query("SELECT c FROM ChatRoom c WHERE c.job.id = :jobId AND c.status = 'ACTIVE'")
    List<ChatRoom> findByJobId(@Param("jobId") Long jobId);

    @Query("SELECT c FROM ChatRoom c WHERE c.candidate.id = :userId OR c.recruiter.id = :userId")
    Page<ChatRoom> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.id = :roomId AND m.createdAt > :since")
    long countMessagesSince(@Param("roomId") Long roomId, @Param("since") java.time.LocalDateTime since);
}