package com.vcraft.domain.model;

import java.time.Instant;
import java.util.UUID;

public record CartItem(
        UUID id,
        UUID cartId,
        UUID productId,
        int quantity,
        Instant createdAt
) {}
