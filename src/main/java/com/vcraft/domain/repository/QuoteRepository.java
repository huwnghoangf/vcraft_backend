package com.vcraft.domain.repository;

import com.vcraft.domain.model.QuoteRequest;
import com.vcraft.domain.model.QuoteStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRepository {
    Optional<QuoteRequest> findById(UUID id);
    QuoteRequest save(QuoteRequest quote);
    void deleteById(UUID id);
    List<QuoteRequest> findByBuyerId(UUID buyerId, QuoteStatus status, int page, int limit);
    long countByBuyerId(UUID buyerId, QuoteStatus status);
    List<QuoteRequest> findIncomingForArtisan(UUID artisanId, QuoteStatus status, int page, int limit);
    long countIncomingForArtisan(UUID artisanId, QuoteStatus status);
    List<QuoteRequest> findAll(QuoteStatus status, int page, int limit);
    long countAll(QuoteStatus status);
}
