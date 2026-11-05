package com.vcraft.domain.service;

import com.vcraft.domain.exception.ForbiddenException;
import com.vcraft.domain.exception.InvalidInputException;
import com.vcraft.domain.exception.NotFoundException;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.Review;
import com.vcraft.domain.repository.ReviewRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public List<Review> getProductReviews(UUID productId, int page, int limit) {
        return reviewRepository.findByProductId(productId, page, limit);
    }

    public long countProductReviews(UUID productId) {
        return reviewRepository.countByProductId(productId);
    }

    public Double getProductAvgRating(UUID productId) {
        return reviewRepository.calculateAvgRatingForProduct(productId);
    }

    public Review createProductReview(Caller caller, UUID productId, int rating, String comment, List<String> images) {
        UUID userId = caller.requireUserId();
        validateRating(rating);

        Review review = new Review(
                UUID.randomUUID(),
                userId,
                productId,
                null,
                rating,
                comment,
                images != null ? images : List.of(),
                Instant.now(),
                Instant.now()
        );
        return reviewRepository.save(review);
    }

    public Review updateReview(Caller caller, UUID reviewId, int rating, String comment) {
        Review current = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đánh giá"));

        if (!current.userId().equals(caller.requireUserId())) {
            throw new ForbiddenException("Bạn không thể sửa đánh giá của người khác");
        }
        validateRating(rating);

        Review updated = new Review(
                current.id(),
                current.userId(),
                current.productId(),
                current.artisanId(),
                rating,
                comment != null ? comment : current.comment(),
                current.images(),
                current.createdAt(),
                Instant.now()
        );
        return reviewRepository.save(updated);
    }

    public void deleteReview(Caller caller, UUID reviewId) {
        Review current = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đánh giá"));

        if (!caller.isAdmin() && !current.userId().equals(caller.requireUserId())) {
            throw new ForbiddenException("Bạn không thể xóa đánh giá của người khác");
        }
        reviewRepository.deleteById(reviewId);
    }

    public List<Review> getArtisanReviews(UUID artisanId, int page, int limit) {
        return reviewRepository.findByArtisanId(artisanId, page, limit);
    }

    public long countArtisanReviews(UUID artisanId) {
        return reviewRepository.countByArtisanId(artisanId);
    }

    public Double getArtisanAvgRating(UUID artisanId) {
        return reviewRepository.calculateAvgRatingForArtisan(artisanId);
    }

    public Review createArtisanReview(Caller caller, UUID artisanId, int rating, String comment) {
        UUID userId = caller.requireUserId();
        validateRating(rating);

        Review review = new Review(
                UUID.randomUUID(),
                userId,
                null,
                artisanId,
                rating,
                comment,
                List.of(),
                Instant.now(),
                Instant.now()
        );
        return reviewRepository.save(review);
    }

    private void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new InvalidInputException("Điểm đánh giá phải từ 1 đến 5 sao");
        }
    }
}
