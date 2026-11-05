package com.vcraft.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArtisanProfile(
        UUID id,
        UUID userId,
        String workshopName,
        String ownerName,
        String phone,
        String address,
        String district,
        Double latitude,
        Double longitude,
        boolean openStatus,
        String description,
        String familyHistory,
        List<String> certificates,
        List<String> images,
        ArtisanStatus status,
        String rejectReason,
        Instant createdAt,
        Instant updatedAt
) {
    public boolean isApproved() {
        return status == ArtisanStatus.APPROVED;
    }
}
