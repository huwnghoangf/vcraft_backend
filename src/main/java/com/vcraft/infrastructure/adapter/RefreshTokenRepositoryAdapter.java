package com.vcraft.infrastructure.adapter;

import com.vcraft.domain.repository.RefreshTokenRepository;
import com.vcraft.infrastructure.entity.RefreshTokenEntity;
import com.vcraft.infrastructure.repository.RefreshTokenJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void save(UUID userId, String token, Instant expiresAt) {
        jpaRepository.deleteByUserId(userId);
        RefreshTokenEntity entity = new RefreshTokenEntity(
                UUID.randomUUID(),
                userId,
                token,
                expiresAt,
                Instant.now()
        );
        jpaRepository.save(entity);
    }

    @Override
    public Optional<UUID> findUserIdByToken(String token) {
        return jpaRepository.findByToken(token)
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .map(RefreshTokenEntity::getUserId);
    }

    @Override
    @Transactional
    public void deleteByToken(String token) {
        jpaRepository.deleteByToken(token);
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }
}
