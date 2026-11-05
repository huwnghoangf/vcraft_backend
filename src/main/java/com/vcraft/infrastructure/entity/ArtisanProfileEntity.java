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
@Table(name = "artisan_profiles")
public class ArtisanProfileEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "workshop_name", nullable = false)
    private String workshopName;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    private String phone;

    @Column(nullable = false)
    private String address;

    private String district;
    private Double latitude;
    private Double longitude;

    @Column(name = "open_status", nullable = false)
    private boolean openStatus;

    private String description;

    @Column(name = "family_history")
    private String familyHistory;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonListConverter.class)
    private List<String> certificates;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonListConverter.class)
    private List<String> images;

    @Column(nullable = false)
    private String status;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ArtisanProfileEntity() {}

    public ArtisanProfileEntity(UUID id, UUID userId, String workshopName, String ownerName,
                                String phone, String address, String district, Double latitude,
                                Double longitude, boolean openStatus, String description,
                                String familyHistory, List<String> certificates, List<String> images,
                                String status, String rejectReason, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.workshopName = workshopName;
        this.ownerName = ownerName;
        this.phone = phone;
        this.address = address;
        this.district = district;
        this.latitude = latitude;
        this.longitude = longitude;
        this.openStatus = openStatus;
        this.description = description;
        this.familyHistory = familyHistory;
        this.certificates = certificates;
        this.images = images;
        this.status = status;
        this.rejectReason = rejectReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getWorkshopName() { return workshopName; }
    public void setWorkshopName(String workshopName) { this.workshopName = workshopName; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public boolean isOpenStatus() { return openStatus; }
    public void setOpenStatus(boolean openStatus) { this.openStatus = openStatus; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFamilyHistory() { return familyHistory; }
    public void setFamilyHistory(String familyHistory) { this.familyHistory = familyHistory; }
    public List<String> getCertificates() { return certificates; }
    public void setCertificates(List<String> certificates) { this.certificates = certificates; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
