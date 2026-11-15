package com.vcraft.infrastructure.repository;

import com.vcraft.infrastructure.entity.QuoteRequestEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteJpaRepository extends JpaRepository<QuoteRequestEntity, UUID> {
    @Override
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<QuoteRequestEntity> findById(UUID id);
    @Query("SELECT q FROM QuoteRequestEntity q WHERE q.buyerId = :buyerId AND (:status IS NULL OR q.status = :status)")
    Page<QuoteRequestEntity> findByBuyerIdAndStatus(@Param("buyerId") UUID buyerId, @Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(q) FROM QuoteRequestEntity q WHERE q.buyerId = :buyerId AND (:status IS NULL OR q.status = :status)")
    long countByBuyerIdAndStatus(@Param("buyerId") UUID buyerId, @Param("status") String status);

    @Query(value = "SELECT * FROM quote_requests q WHERE q.status NOT IN ('DRAFT', 'ALLOCATED') AND (:status IS NULL OR q.status = :status) AND (jsonb_array_length(q.target_artisan_ids) = 0 OR q.target_artisan_ids @> CAST(:target AS jsonb))", nativeQuery = true)
    Page<QuoteRequestEntity> findIncomingForArtisans(@Param("target") String target, @Param("status") String status, Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM quote_requests q WHERE q.status NOT IN ('DRAFT', 'ALLOCATED') AND (:status IS NULL OR q.status = :status) AND (jsonb_array_length(q.target_artisan_ids) = 0 OR q.target_artisan_ids @> CAST(:target AS jsonb))", nativeQuery = true)
    long countIncomingForArtisans(@Param("target") String target, @Param("status") String status);

    @Query("SELECT q FROM QuoteRequestEntity q WHERE (:status IS NULL OR q.status = :status)")
    Page<QuoteRequestEntity> findAllByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(q) FROM QuoteRequestEntity q WHERE (:status IS NULL OR q.status = :status)")
    long countAllByStatus(@Param("status") String status);
}
