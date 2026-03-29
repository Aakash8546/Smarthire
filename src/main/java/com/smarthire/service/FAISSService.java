
package com.smarthire.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthire.entity.Embedding;
import com.smarthire.repository.EmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FAISSService {

    private final MLServiceClient mlServiceClient;
    private final EmbeddingRepository embeddingRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${faiss.index-cache-ttl:3600}")
    private int cacheTtl;

    private static final String FAISS_CACHE_PREFIX = "faiss:search:";

    @PostConstruct
    public void init() {
        log.info("FAISS Service initialized");
    }

    public List<Map<String, Object>> findSimilarCandidates(Long jobId, String jobDescription, int topK) {
        String cacheKey = FAISS_CACHE_PREFIX + "job:" + jobId + ":top:" + topK;


        List<Map<String, Object>> cached = getCachedResults(cacheKey);
        if (cached != null) {
            log.info("Returning cached FAISS results for job: {}", jobId);
            return cached;
        }

        try {

            float[] jobEmbedding = mlServiceClient.getEmbedding(jobDescription);


            Map<String, Object> query = new HashMap<>();
            query.put("embedding", jobEmbedding);
            query.put("top_k", topK);
            query.put("entity_type", "RESUME");


            List<Map<String, Object>> similar = queryFAISS(query);


            cacheResults(cacheKey, similar);

            return similar;
        } catch (Exception e) {
            log.error("Error finding similar candidates via FAISS", e);
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> queryFAISS(Map<String, Object> query) {

        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getCachedResults(String key) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.convertValue(cached, List.class);
            }
        } catch (Exception e) {
            log.warn("Error retrieving from cache", e);
        }
        return null;
    }

    private void cacheResults(String key, List<Map<String, Object>> results) {
        try {
            redisTemplate.opsForValue().set(key, results, cacheTtl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Error caching results", e);
        }
    }

    public void syncEmbeddingToFAISS(Long entityId, String entityType, String text) {
        try {
            float[] embedding = mlServiceClient.getEmbedding(text);


            Embedding embeddingEntity = new Embedding();
            embeddingEntity.setEntityType(entityType);
            embeddingEntity.setEntityId(entityId);
            embeddingEntity.setEmbeddingVector(Arrays.toString(embedding));
            embeddingRepository.save(embeddingEntity);


            Map<String, Object> syncMessage = new HashMap<>();
            syncMessage.put("entity_id", entityId);
            syncMessage.put("entity_type", entityType);
            syncMessage.put("embedding", embedding);

          

            log.info("Synced embedding for {}_{} to FAISS", entityType, entityId);
        } catch (Exception e) {
            log.error("Error syncing embedding to FAISS", e);
        }
    }
}