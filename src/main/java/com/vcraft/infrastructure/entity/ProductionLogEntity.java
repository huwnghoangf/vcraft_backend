package com.vcraft.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "production_logs")
public class ProductionLogEntity {

    @Id
    private UUID id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(nullable = false)
    private String stage;

    private String note;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonListConverter.class)
    private List<String> images;

    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ProductionLogEntity() {}

    public ProductionLogEntity(UUID id, UUID batchId, String stage, String note,
                               List<String> images, Instant loggedAt, Instant createdAt) {
        this.id = id;
        this.batchId = batchId;
        this.stage = stage;
        this.note = note;
        this.images = images;
        this.loggedAt = loggedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBatchId() { return batchId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public Instant getLoggedAt() { return loggedAt; }
    public void setLoggedAt(Instant loggedAt) { this.loggedAt = loggedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
