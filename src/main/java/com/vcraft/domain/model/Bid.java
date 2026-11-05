package com.vcraft.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Bid(
        UUID id,
        UUID quoteRequestId,
        UUID artisanId,
        BigDecimal pricePerUnit,
        BigDecimal totalPrice,
        int estimatedDays,
        String note,
        String deliveryTerms,
        BidStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
