package com.vcraft.infrastructure.adapter;

import com.vcraft.domain.model.Bid;
import com.vcraft.domain.model.BidStatus;
import com.vcraft.domain.repository.BidRepository;
import com.vcraft.infrastructure.entity.BidEntity;
import com.vcraft.infrastructure.repository.BidJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BidRepositoryAdapter implements BidRepository {

    private final BidJpaRepository jpaRepository;

    public BidRepositoryAdapter(BidJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Bid> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Bid> findByQuoteRequestId(UUID quoteRequestId) {
        return jpaRepository.findByQuoteRequestId(quoteRequestId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Bid> findByQuoteRequestIdAndArtisanId(UUID quoteRequestId, UUID artisanId) {
        return jpaRepository.findByQuoteRequestIdAndArtisanId(quoteRequestId, artisanId).map(this::toDomain);
    }

    @Override
    public Bid save(Bid bid) {
        BidEntity entity = toEntity(bid);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void updateStatusForOtherBids(UUID quoteRequestId, UUID acceptedBidId, BidStatus status) {
        jpaRepository.updateOtherBidsStatus(quoteRequestId, acceptedBidId, status.name());
    }

    private Bid toDomain(BidEntity entity) {
        return new Bid(
                entity.getId(),
                entity.getQuoteRequestId(),
                entity.getArtisanId(),
                entity.getPricePerUnit(),
                entity.getTotalPrice(),
                entity.getEstimatedDays(),
                entity.getNote(),
                entity.getDeliveryTerms(),
                BidStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private BidEntity toEntity(Bid domain) {
        return new BidEntity(
                domain.id(),
                domain.quoteRequestId(),
                domain.artisanId(),
                domain.pricePerUnit(),
                domain.totalPrice(),
                domain.estimatedDays(),
                domain.note(),
                domain.deliveryTerms(),
                domain.status().name(),
                domain.createdAt(),
                domain.updatedAt()
        );
    }
}
