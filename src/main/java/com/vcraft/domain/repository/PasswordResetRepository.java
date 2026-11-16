package com.vcraft.domain.repository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
public interface PasswordResetRepository {
    record Token(String hash, UUID userId, Instant createdAt, Instant expiresAt) {}
    Optional<Token> find(String hash);
    boolean issuedSince(UUID userId, Instant since);
    void invalidateAll(UUID userId, Instant now);
    void save(Token token);
    boolean consume(String hash, Instant now);
}
