package com.vcraft.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuoteRequest(
        UUID id,
        UUID buyerId,
        UUID categoryId,
        String title,
        String description,
        int quantity,
        String unit,
        Instant deadline,
        QuoteStatus status,
        List<UUID> targetArtisanIds,
        String deliveryNote,
        List<String> deliveryImages,
        UUID acceptedBidId,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public boolean isDraft() {
        return status == QuoteStatus.DRAFT;
    }

    public boolean canBeSubmitted() {
        return status == QuoteStatus.DRAFT;
    }

    public boolean canReceiveBids() {
        return status == QuoteStatus.SENT || status == QuoteStatus.BIDDING;
    }
}
