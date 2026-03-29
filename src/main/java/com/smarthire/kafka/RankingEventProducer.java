
package com.smarthire.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthire.dto.RankedCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String RANKING_TOPIC = "ranking-events";
    private static final String SCORE_UPDATE_TOPIC = "score-updates";
    private static final String ANALYTICS_TOPIC = "analytics-events";

    public void sendRankingEvent(Long jobId, List<RankedCandidate> rankings) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("job_id", jobId);
            event.put("rankings", rankings);
            event.put("timestamp", LocalDateTime.now());
            event.put("event_type", "RANKING_COMPLETED");

            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(RANKING_TOPIC, String.valueOf(jobId), message);
            log.info("Sent ranking event for job: {}", jobId);
        } catch (JsonProcessingException e) {
            log.error("Error serializing ranking event", e);
        }
    }

    public void sendScoreUpdateEvent(Long applicationId, Long jobId, Long userId, Double score) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("application_id", applicationId);
            event.put("job_id", jobId);
            event.put("user_id", userId);
            event.put("score", score);
            event.put("timestamp", LocalDateTime.now());
            event.put("event_type", "SCORE_UPDATED");

            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(SCORE_UPDATE_TOPIC, String.valueOf(jobId), message);
            log.info("Sent score update event for application: {}", applicationId);
        } catch (JsonProcessingException e) {
            log.error("Error serializing score update event", e);
        }
    }

    public void sendAnalyticsEvent(String eventType, Map<String, Object> data) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("event_type", eventType);
            event.put("data", data);
            event.put("timestamp", LocalDateTime.now());

            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(ANALYTICS_TOPIC, eventType, message);
            log.debug("Sent analytics event: {}", eventType);
        } catch (JsonProcessingException e) {
            log.error("Error serializing analytics event", e);
        }
    }
}