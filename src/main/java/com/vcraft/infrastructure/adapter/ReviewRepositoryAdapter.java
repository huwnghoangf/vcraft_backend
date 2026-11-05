package com.vcraft.infrastructure.adapter;

import com.vcraft.domain.model.Review;
import com.vcraft.domain.repository.ReviewRepository;
import com.vcraft.infrastructure.entity.ReviewEntity;
import com.vcraft.infrastructure.repository.ReviewJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class ReviewRepositoryAdapter implements ReviewRepository {

    private final ReviewJpaRepository jpaRepository;

    public ReviewRepositoryAdapter(ReviewJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Review> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Review save(Review review) {
        ReviewEntity entity = toEntity(review);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<Review> findByProductId(UUID productId, int page, int limit) {
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt"));
        return jpaRepository.findByProductId(productId, pageable).getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByProductId(UUID productId) {
        return jpaRepository.countByProductId(productId);
    }

    @Override
    public Double calculateAvgRatingForProduct(UUID productId) {
        Double avg = jpaRepository.getAvgRatingForProduct(productId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Override
    public List<Review> findByArtisanId(UUID artisanId, int page, int limit) {
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt"));
        return jpaRepository.findByArtisanId(artisanId, pageable).getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByArtisanId(UUID artisanId) {
        return jpaRepository.countByArtisanId(artisanId);
    }

    @Override
    public Double calculateAvgRatingForArtisan(UUID artisanId) {
        Double avg = jpaRepository.getAvgRatingForArtisan(artisanId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    private Review toDomain(ReviewEntity entity) {
        return new Review(
                entity.getId(),
                entity.getUserId(),
                entity.getProductId(),
                entity.getArtisanId(),
                entity.getRating(),
                entity.getComment(),
                entity.getImages(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ReviewEntity toEntity(Review domain) {
        return new ReviewEntity(
                domain.id(),
                domain.userId(),
                domain.productId(),
                domain.artisanId(),
                domain.rating(),
                domain.comment(),
                domain.images(),
                domain.createdAt(),
                domain.updatedAt()
        );
    }
}
