package com.vcraft.infrastructure.repository;

import com.vcraft.infrastructure.entity.QuoteRequestEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteJpaRepository extends JpaRepository<QuoteRequestEntity, UUID> {
    @Query("SELECT q FROM QuoteRequestEntity q WHERE q.buyerId = :buyerId AND (:status IS NULL OR q.status = :status)")
    Page<QuoteRequestEntity> findByBuyerIdAndStatus(@Param("buyerId") UUID buyerId, @Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(q) FROM QuoteRequestEntity q WHERE q.buyerId = :buyerId AND (:status IS NULL OR q.status = :status)")
    long countByBuyerIdAndStatus(@Param("buyerId") UUID buyerId, @Param("status") String status);

    @Query("SELECT q FROM QuoteRequestEntity q WHERE (q.status = 'SENT' OR q.status = 'BIDDING') AND (:status IS NULL OR q.status = :status)")
    Page<QuoteRequestEntity> findIncomingForArtisans(@Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(q) FROM QuoteRequestEntity q WHERE (q.status = 'SENT' OR q.status = 'BIDDING') AND (:status IS NULL OR q.status = :status)")
    long countIncomingForArtisans(@Param("status") String status);

    @Query("SELECT q FROM QuoteRequestEntity q WHERE (:status IS NULL OR q.status = :status)")
    Page<QuoteRequestEntity> findAllByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(q) FROM QuoteRequestEntity q WHERE (:status IS NULL OR q.status = :status)")
    long countAllByStatus(@Param("status") String status);
}
