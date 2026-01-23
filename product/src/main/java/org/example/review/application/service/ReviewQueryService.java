package org.example.review.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.groom.e_commerce.review.domain.entity.ProductRatingEntity;
import com.groom.e_commerce.review.domain.entity.ReviewEntity;
import com.groom.e_commerce.review.domain.repository.ProductRatingRepository;
import com.groom.e_commerce.review.domain.repository.ReviewRepository;
import com.groom.e_commerce.review.infrastructure.redis.ReviewReadModel;
import com.groom.e_commerce.review.infrastructure.redis.ReviewRedisRepository;
import com.groom.e_commerce.review.presentation.dto.response.PaginationResponse;
import com.groom.e_commerce.review.presentation.dto.response.ProductReviewResponse;
import com.groom.e_commerce.review.presentation.dto.response.ReviewResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewQueryService {

    private final ReviewRedisRepository reviewRedisRepository;
    private final ReviewRepository reviewRepository;
    private final ProductRatingRepository productRatingRepository;

    public ProductReviewResponse getProductReviews(
        UUID productId,
        int page,
        int size
    ) {
        // 1️⃣ Redis 조회
        List<ReviewReadModel> cached =
            reviewRedisRepository.findByProductId(productId, page, size);

        if (!cached.isEmpty()) {
            return buildResponseFromCache(productId, cached);
        }

        // 2️⃣ Redis miss → DB 조회
        Page<ReviewEntity> pageResult =
            reviewRepository.findAllByProductId(
                productId,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
            );

        // 3️⃣ Redis 저장
        pageResult.forEach(this::saveToRedis);

        return buildResponseFromDb(productId, pageResult);
    }

    private ProductReviewResponse buildResponseFromCache(
        UUID productId,
        List<ReviewReadModel> cached
    ) {
        ProductRatingEntity rating =
            productRatingRepository.findByProductId(productId)
                .orElseGet(() -> new ProductRatingEntity(productId));

        return ProductReviewResponse.builder()
            .avgRating(rating.getAvgRating())
            .reviewCount(rating.getReviewCount())
            .aiReview(rating.getAiReview())
            .reviews(
                cached.stream()
                    .map(ReviewResponse::fromReadModel)
                    .toList()
            )
            .build();
    }
    private ProductReviewResponse buildResponseFromDb(
        UUID productId,
        Page<ReviewEntity> pageResult
    ) {
        ProductRatingEntity rating =
            productRatingRepository.findByProductId(productId)
                .orElseGet(() -> new ProductRatingEntity(productId));

        return ProductReviewResponse.builder()
            .avgRating(rating.getAvgRating())
            .reviewCount(rating.getReviewCount())
            .aiReview(rating.getAiReview())
            .reviews(
                pageResult.getContent().stream()
                    .map(ReviewResponse::fromEntity)
                    .toList()
            )
            .pagination(
                PaginationResponse.builder()
                    .totalElements(pageResult.getTotalElements())
                    .totalPages(pageResult.getTotalPages())
                    .currentPage(pageResult.getNumber())
                    .isLast(pageResult.isLast())
                    .build()
            )
            .build();
    }

    private void saveToRedis(ReviewEntity review) {
        ReviewReadModel readModel = ReviewReadModel.builder()
            .reviewId(review.getReviewId())
            .productId(review.getProductId())
            .userId(review.getUserId())
            .rating(review.getRating())
            .content(review.getContent())
            .createdAt(review.getCreatedAt())
            .build();

        reviewRedisRepository.save(readModel);
    }
}

