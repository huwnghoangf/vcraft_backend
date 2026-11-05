package com.vcraft.domain.repository;

import com.vcraft.domain.model.Batch;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BatchRepository {
    Optional<Batch> findById(UUID id);
    Optional<Batch> findByQrToken(String qrToken);
    Batch save(Batch batch);
    List<Batch> findByArtisanId(UUID artisanId, String status, int page, int limit);
    long countByArtisanId(UUID artisanId, String status);
}
