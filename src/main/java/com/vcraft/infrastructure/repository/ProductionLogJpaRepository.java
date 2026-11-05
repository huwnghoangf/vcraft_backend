package com.vcraft.infrastructure.repository;

import com.vcraft.infrastructure.entity.ProductionLogEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionLogJpaRepository extends JpaRepository<ProductionLogEntity, UUID> {
    List<ProductionLogEntity> findByBatchIdOrderByLoggedAtAsc(UUID batchId);
}
