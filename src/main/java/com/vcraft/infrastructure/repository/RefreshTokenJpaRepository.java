package com.vcraft.infrastructure.repository;

import com.vcraft.infrastructure.entity.RefreshTokenEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshTokenEntity> findByToken(String token);
    void deleteByToken(String token);
    void deleteByUserId(UUID userId);
}
