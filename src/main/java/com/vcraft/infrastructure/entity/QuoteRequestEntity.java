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
@Table(name = "quote_requests")
public class QuoteRequestEntity {

    @Id
    private UUID id;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    private Instant deadline;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonUuidListConverter.class)
    private List<UUID> targetArtisanIds;

    @Column(name = "delivery_note")
    private String deliveryNote;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonListConverter.class)
    private List<String> deliveryImages;

    @Column(name = "accepted_bid_id")
    private UUID acceptedBidId;

    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public QuoteRequestEntity() {}

    public QuoteRequestEntity(UUID id, UUID buyerId, UUID categoryId, String title,
                              String description, int quantity, String unit, Instant deadline,
                              String status, List<UUID> targetArtisanIds, String deliveryNote,
                              List<String> deliveryImages, UUID acceptedBidId, int version,
                              Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.buyerId = buyerId;
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.quantity = quantity;
        this.unit = unit;
        this.deadline = deadline;
        this.status = status;
        this.targetArtisanIds = targetArtisanIds;
        this.deliveryNote = deliveryNote;
        this.deliveryImages = deliveryImages;
        this.acceptedBidId = acceptedBidId;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBuyerId() { return buyerId; }
    public void setBuyerId(UUID buyerId) { this.buyerId = buyerId; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Instant getDeadline() { return deadline; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<UUID> getTargetArtisanIds() { return targetArtisanIds; }
    public void setTargetArtisanIds(List<UUID> targetArtisanIds) { this.targetArtisanIds = targetArtisanIds; }
    public String getDeliveryNote() { return deliveryNote; }
    public void setDeliveryNote(String deliveryNote) { this.deliveryNote = deliveryNote; }
    public List<String> getDeliveryImages() { return deliveryImages; }
    public void setDeliveryImages(List<String> deliveryImages) { this.deliveryImages = deliveryImages; }
    public UUID getAcceptedBidId() { return acceptedBidId; }
    public void setAcceptedBidId(UUID acceptedBidId) { this.acceptedBidId = acceptedBidId; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
