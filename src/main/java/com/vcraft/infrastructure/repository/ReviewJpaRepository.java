package com.vcraft.infrastructure.repository;

import com.vcraft.infrastructure.entity.ReviewEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, UUID> {
    Page<ReviewEntity> findByProductId(UUID productId, Pageable pageable);
    long countByProductId(UUID productId);

    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.productId = :productId")
    Double getAvgRatingForProduct(@Param("productId") UUID productId);

    Page<ReviewEntity> findByArtisanId(UUID artisanId, Pageable pageable);
    long countByArtisanId(UUID artisanId);

    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.artisanId = :artisanId")
    Double getAvgRatingForArtisan(@Param("artisanId") UUID artisanId);
}
