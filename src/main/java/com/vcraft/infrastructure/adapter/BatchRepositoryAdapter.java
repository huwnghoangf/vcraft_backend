package com.vcraft.infrastructure.adapter;

import com.vcraft.domain.model.Batch;
import com.vcraft.domain.model.BatchStatus;
import com.vcraft.domain.repository.BatchRepository;
import com.vcraft.infrastructure.entity.BatchEntity;
import com.vcraft.infrastructure.repository.BatchJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class BatchRepositoryAdapter implements BatchRepository {

    private final BatchJpaRepository jpaRepository;

    public BatchRepositoryAdapter(BatchJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Batch> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Batch> findByQrToken(String qrToken) {
        return jpaRepository.findByQrToken(qrToken).map(this::toDomain);
    }

    @Override
    public Batch save(Batch batch) {
        BatchEntity entity = toEntity(batch);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<Batch> findByArtisanId(UUID artisanId, String status, int page, int limit) {
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt"));
        return jpaRepository.findByArtisanIdAndStatus(artisanId, status, pageable).getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByArtisanId(UUID artisanId, String status) {
        return jpaRepository.countByArtisanIdAndStatus(artisanId, status);
    }

    private Batch toDomain(BatchEntity entity) {
        return new Batch(
                entity.getId(),
                entity.getArtisanId(),
                entity.getName(),
                entity.getDescription(),
                entity.getQuantity(),
                BatchStatus.valueOf(entity.getStatus()),
                entity.getQrCodeUrl(),
                entity.getQrToken(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private BatchEntity toEntity(Batch domain) {
        return new BatchEntity(
                domain.id(),
                domain.artisanId(),
                domain.name(),
                domain.description(),
                domain.quantity(),
                domain.status().name(),
                domain.qrCodeUrl(),
                domain.qrToken(),
                domain.createdAt(),
                domain.updatedAt()
        );
    }
}
