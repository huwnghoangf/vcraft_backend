package com.vcraft.infrastructure.adapter;

import com.vcraft.domain.model.QuoteRequest;
import com.vcraft.domain.model.QuoteStatus;
import com.vcraft.domain.repository.QuoteRepository;
import com.vcraft.infrastructure.entity.QuoteRequestEntity;
import com.vcraft.infrastructure.repository.QuoteJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class QuoteRepositoryAdapter implements QuoteRepository {

    private final QuoteJpaRepository jpaRepository;

    public QuoteRepositoryAdapter(QuoteJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<QuoteRequest> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public QuoteRequest save(QuoteRequest quote) {
        QuoteRequestEntity entity = toEntity(quote);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<QuoteRequest> findByBuyerId(UUID buyerId, QuoteStatus status, int page, int limit) {
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt"));
        String statusStr = status != null ? status.name() : null;
        return jpaRepository.findByBuyerIdAndStatus(buyerId, statusStr, pageable).getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByBuyerId(UUID buyerId, QuoteStatus status) {
        String statusStr = status != null ? status.name() : null;
        return jpaRepository.countByBuyerIdAndStatus(buyerId, statusStr);
    }

    @Override
    public List<QuoteRequest> findIncomingForArtisan(UUID artisanId, QuoteStatus status, int page, int limit) {
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt"));
        String statusStr = status != null ? status.name() : null;
        return jpaRepository.findIncomingForArtisans(statusStr, pageable).getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countIncomingForArtisan(UUID artisanId, QuoteStatus status) {
        String statusStr = status != null ? status.name() : null;
        return jpaRepository.countIncomingForArtisans(statusStr);
    }

    @Override
    public List<QuoteRequest> findAll(QuoteStatus status, int page, int limit) {
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt"));
        String statusStr = status != null ? status.name() : null;
        return jpaRepository.findAllByStatus(statusStr, pageable).getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countAll(QuoteStatus status) {
        String statusStr = status != null ? status.name() : null;
        return jpaRepository.countAllByStatus(statusStr);
    }

    private QuoteRequest toDomain(QuoteRequestEntity entity) {
        return new QuoteRequest(
                entity.getId(),
                entity.getBuyerId(),
                entity.getCategoryId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getQuantity(),
                entity.getUnit(),
                entity.getDeadline(),
                QuoteStatus.valueOf(entity.getStatus()),
                entity.getTargetArtisanIds(),
                entity.getDeliveryNote(),
                entity.getDeliveryImages(),
                entity.getAcceptedBidId(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private QuoteRequestEntity toEntity(QuoteRequest domain) {
        return new QuoteRequestEntity(
                domain.id(),
                domain.buyerId(),
                domain.categoryId(),
                domain.title(),
                domain.description(),
                domain.quantity(),
                domain.unit(),
                domain.deadline(),
                domain.status().name(),
                domain.targetArtisanIds(),
                domain.deliveryNote(),
                domain.deliveryImages(),
                domain.acceptedBidId(),
                domain.version(),
                domain.createdAt(),
                domain.updatedAt()
        );
    }
}
