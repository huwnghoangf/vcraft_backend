package com.vcraft.domain.model;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String email,
        String passwordHash,
        String fullName,
        String phone,
        Role role,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
