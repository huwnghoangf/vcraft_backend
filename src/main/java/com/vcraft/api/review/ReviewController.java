package com.vcraft.api.review;

import com.vcraft.api.CallerResolver;
import com.vcraft.api.dto.PageResponse;
import com.vcraft.api.review.dto.CreateReviewRequest;
import com.vcraft.api.review.dto.UpdateReviewRequest;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.Review;
import com.vcraft.domain.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "10. Reviews & Ratings", description = "Các API đánh giá sản phẩm và xếp hạng xưởng gốm (1-5 sao)")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // Product reviews
    @GetMapping("/products/{id}/reviews")
    @Operation(summary = "Lấy danh sách đánh giá của sản phẩm kèm điểm trung bình")
    public ResponseEntity<Map<String, Object>> getProductReviews(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<Review> data = reviewService.getProductReviews(id, page, limit);
        long total = reviewService.countProductReviews(id);
        Double avgRating = reviewService.getProductAvgRating(id);

        return ResponseEntity.ok(Map.of(
                "reviews", PageResponse.of(data, page, limit, total),
                "avgRating", avgRating != null ? avgRating : 5.0
        ));
    }

    @PostMapping("/products/{id}/reviews")
    @Operation(summary = "Buyer viết đánh giá sản phẩm")
    public ResponseEntity<Review> createProductReview(Authentication authentication,
                                                      @PathVariable UUID id,
                                                      @Valid @RequestBody CreateReviewRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        Review review = reviewService.createProductReview(caller, id, request.rating(), request.comment(), request.images());
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @PutMapping("/products/{id}/reviews/{reviewId}")
    @Operation(summary = "Buyer chỉnh sửa đánh giá sản phẩm đã viết")
    public ResponseEntity<Review> updateProductReview(Authentication authentication,
                                                      @PathVariable UUID id,
                                                      @PathVariable UUID reviewId,
                                                      @Valid @RequestBody UpdateReviewRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(reviewService.updateReview(caller, reviewId, request.rating(), request.comment()));
    }

    @DeleteMapping("/products/{id}/reviews/{reviewId}")
    @Operation(summary = "Buyer xóa đánh giá sản phẩm")
    public ResponseEntity<Map<String, String>> deleteProductReview(Authentication authentication,
                                                                   @PathVariable UUID id,
                                                                   @PathVariable UUID reviewId) {
        Caller caller = CallerResolver.resolve(authentication);
        reviewService.deleteReview(caller, reviewId);
        return ResponseEntity.ok(Map.of("message", "Deleted review successfully"));
    }

    // Artisan reviews
    @GetMapping("/artisans/{id}/reviews")
    @Operation(summary = "Lấy danh sách đánh giá của xưởng gốm kèm điểm trung bình")
    public ResponseEntity<Map<String, Object>> getArtisanReviews(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<Review> data = reviewService.getArtisanReviews(id, page, limit);
        long total = reviewService.countArtisanReviews(id);
        Double avgRating = reviewService.getArtisanAvgRating(id);

        return ResponseEntity.ok(Map.of(
                "reviews", PageResponse.of(data, page, limit, total),
                "avgRating", avgRating != null ? avgRating : 5.0
        ));
    }

    @PostMapping("/artisans/{id}/reviews")
    @Operation(summary = "Buyer đánh giá xưởng gốm")
    public ResponseEntity<Review> createArtisanReview(Authentication authentication,
                                                      @PathVariable UUID id,
                                                      @Valid @RequestBody CreateReviewRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        Review review = reviewService.createArtisanReview(caller, id, request.rating(), request.comment());
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }
}
