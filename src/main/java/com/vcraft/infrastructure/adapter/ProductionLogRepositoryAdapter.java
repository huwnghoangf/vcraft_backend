package com.vcraft.infrastructure.adapter;

import com.vcraft.domain.model.ProductionLog;
import com.vcraft.domain.repository.ProductionLogRepository;
import com.vcraft.infrastructure.entity.ProductionLogEntity;
import com.vcraft.infrastructure.repository.ProductionLogJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProductionLogRepositoryAdapter implements ProductionLogRepository {

    private final ProductionLogJpaRepository jpaRepository;

    public ProductionLogRepositoryAdapter(ProductionLogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ProductionLog> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ProductionLog> findByBatchIdOrderByLoggedAtAsc(UUID batchId) {
        return jpaRepository.findByBatchIdOrderByLoggedAtAsc(batchId).stream().map(this::toDomain).toList();
    }

    @Override
    public ProductionLog save(ProductionLog log) {
        ProductionLogEntity entity = new ProductionLogEntity(
                log.id(),
                log.batchId(),
                log.stage(),
                log.note(),
                log.images(),
                log.loggedAt(),
                log.createdAt()
        );
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private ProductionLog toDomain(ProductionLogEntity entity) {
        return new ProductionLog(
                entity.getId(),
                entity.getBatchId(),
                entity.getStage(),
                entity.getNote(),
                entity.getImages(),
                entity.getLoggedAt(),
                entity.getCreatedAt()
        );
    }
}
