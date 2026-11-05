package com.vcraft.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Order(
        UUID id,
        UUID buyerId,
        String shippingAddress,
        String paymentMethod,
        String note,
        OrderStatus status,
        BigDecimal totalAmount,
        String cancelReason,
        List<OrderItem> items,
        Instant createdAt,
        Instant updatedAt
) {}
