package com.vcraft.infrastructure.repository;

import com.vcraft.infrastructure.entity.BidEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidJpaRepository extends JpaRepository<BidEntity, UUID> {
    List<BidEntity> findByQuoteRequestId(UUID quoteRequestId);
    Optional<BidEntity> findByQuoteRequestIdAndArtisanId(UUID quoteRequestId, UUID artisanId);

    @Modifying
    @Query("UPDATE BidEntity b SET b.status = :status WHERE b.quoteRequestId = :quoteRequestId AND b.id != :acceptedBidId")
    void updateOtherBidsStatus(@Param("quoteRequestId") UUID quoteRequestId,
                               @Param("acceptedBidId") UUID acceptedBidId,
                               @Param("status") String status);
}
