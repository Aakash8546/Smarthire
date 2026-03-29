
package com.smarthire.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "embeddings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"entity_type", "entity_id"})
})
@Data
public class Embedding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "embedding_vector", columnDefinition = "vector(768)")
    private String embeddingVector;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}