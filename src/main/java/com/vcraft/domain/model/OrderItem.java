package com.vcraft.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItem(
        UUID id,
        UUID orderId,
        UUID productId,
        UUID artisanId,
        String productName,
        BigDecimal price,
        int quantity
) {}
