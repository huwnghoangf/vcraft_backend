package com.vcraft.domain.repository;

import com.vcraft.domain.model.Review;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository {
    Optional<Review> findById(UUID id);
    Review save(Review review);
    void deleteById(UUID id);
    List<Review> findByProductId(UUID productId, int page, int limit);
    long countByProductId(UUID productId);
    Double calculateAvgRatingForProduct(UUID productId);
    List<Review> findByArtisanId(UUID artisanId, int page, int limit);
    long countByArtisanId(UUID artisanId);
    Double calculateAvgRatingForArtisan(UUID artisanId);
}
