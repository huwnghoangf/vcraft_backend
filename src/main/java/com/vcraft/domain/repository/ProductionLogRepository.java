package com.vcraft.domain.repository;

import com.vcraft.domain.model.ProductionLog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductionLogRepository {
    Optional<ProductionLog> findById(UUID id);
    List<ProductionLog> findByBatchIdOrderByLoggedAtAsc(UUID batchId);
    ProductionLog save(ProductionLog log);
    void deleteById(UUID id);
}
