package com.vcraft.infrastructure.repository;

import com.vcraft.infrastructure.entity.BatchEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BatchJpaRepository extends JpaRepository<BatchEntity, UUID> {
    Optional<BatchEntity> findByQrToken(String qrToken);

    @Query("SELECT b FROM BatchEntity b WHERE b.artisanId = :artisanId AND (:status IS NULL OR b.status = :status)")
    Page<BatchEntity> findByArtisanIdAndStatus(@Param("artisanId") UUID artisanId, @Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(b) FROM BatchEntity b WHERE b.artisanId = :artisanId AND (:status IS NULL OR b.status = :status)")
    long countByArtisanIdAndStatus(@Param("artisanId") UUID artisanId, @Param("status") String status);
}
