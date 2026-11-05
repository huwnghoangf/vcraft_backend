package com.vcraft.domain.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    void save(UUID userId, String token, Instant expiresAt);
    Optional<UUID> findUserIdByToken(String token);
    void deleteByToken(String token);
    void deleteByUserId(UUID userId);
}
