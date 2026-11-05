package com.vcraft.infrastructure.repository;

import com.vcraft.infrastructure.entity.OrderEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
    @Query("SELECT o FROM OrderEntity o WHERE o.buyerId = :buyerId AND (:status IS NULL OR o.status = :status)")
    Page<OrderEntity> findByBuyerIdAndStatus(@Param("buyerId") UUID buyerId, @Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.buyerId = :buyerId AND (:status IS NULL OR o.status = :status)")
    long countByBuyerIdAndStatus(@Param("buyerId") UUID buyerId, @Param("status") String status);

    @Query("SELECT DISTINCT o FROM OrderEntity o JOIN o.items i WHERE i.artisanId = :artisanId AND (:status IS NULL OR o.status = :status)")
    Page<OrderEntity> findByArtisanIdAndStatus(@Param("artisanId") UUID artisanId, @Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT o) FROM OrderEntity o JOIN o.items i WHERE i.artisanId = :artisanId AND (:status IS NULL OR o.status = :status)")
    long countByArtisanIdAndStatus(@Param("artisanId") UUID artisanId, @Param("status") String status);
}
