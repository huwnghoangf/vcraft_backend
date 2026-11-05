package com.vcraft.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bids")
public class BidEntity {

    @Id
    private UUID id;

    @Column(name = "quote_request_id", nullable = false)
    private UUID quoteRequestId;

    @Column(name = "artisan_id", nullable = false)
    private UUID artisanId;

    @Column(name = "price_per_unit", nullable = false, precision = 15, scale = 2)
    private BigDecimal pricePerUnit;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "estimated_days", nullable = false)
    private int estimatedDays;

    private String note;

    @Column(name = "delivery_terms")
    private String deliveryTerms;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BidEntity() {}

    public BidEntity(UUID id, UUID quoteRequestId, UUID artisanId, BigDecimal pricePerUnit,
                     BigDecimal totalPrice, int estimatedDays, String note, String deliveryTerms,
                     String status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.quoteRequestId = quoteRequestId;
        this.artisanId = artisanId;
        this.pricePerUnit = pricePerUnit;
        this.totalPrice = totalPrice;
        this.estimatedDays = estimatedDays;
        this.note = note;
        this.deliveryTerms = deliveryTerms;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getQuoteRequestId() { return quoteRequestId; }
    public void setQuoteRequestId(UUID quoteRequestId) { this.quoteRequestId = quoteRequestId; }
    public UUID getArtisanId() { return artisanId; }
    public void setArtisanId(UUID artisanId) { this.artisanId = artisanId; }
    public BigDecimal getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(BigDecimal pricePerUnit) { this.pricePerUnit = pricePerUnit; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public int getEstimatedDays() { return estimatedDays; }
    public void setEstimatedDays(int estimatedDays) { this.estimatedDays = estimatedDays; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getDeliveryTerms() { return deliveryTerms; }
    public void setDeliveryTerms(String deliveryTerms) { this.deliveryTerms = deliveryTerms; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
