package com.vcraft.domain.repository;

import com.vcraft.domain.model.Bid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidRepository {
    Optional<Bid> findById(UUID id);
    List<Bid> findByQuoteRequestId(UUID quoteRequestId);
    Optional<Bid> findByQuoteRequestIdAndArtisanId(UUID quoteRequestId, UUID artisanId);
    Bid save(Bid bid);
    void updateStatusForOtherBids(UUID quoteRequestId, UUID acceptedBidId, com.vcraft.domain.model.BidStatus status);
}
