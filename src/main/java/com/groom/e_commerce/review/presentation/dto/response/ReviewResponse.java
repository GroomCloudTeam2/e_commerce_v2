package com.groom.e_commerce.review.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.groom.e_commerce.review.domain.entity.ReviewCategory;
import com.groom.e_commerce.review.domain.entity.ReviewEntity;
import com.groom.e_commerce.review.infrastructure.redis.ReviewReadModel;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewResponse {
	private UUID reviewId;
	private UUID orderId;
	private UUID productId;
	private UUID userId;
	private Integer rating;
	private String content;
	private LocalDateTime createdAt;
	private ReviewCategory category; // AI가 분류한 카테고리

	public static ReviewResponse fromEntity(ReviewEntity entity) {
		return ReviewResponse.builder()
			.reviewId(entity.getReviewId())
			.orderId(entity.getOrderId())
			.productId(entity.getProductId())
			.userId(entity.getUserId())
			.rating(entity.getRating())
			.content(entity.getContent())
			.category(entity.getCategory())
			.build();
	}

	public static ReviewResponse fromReadModel(ReviewReadModel readModel) {
		return ReviewResponse.builder()
			.reviewId(readModel.getReviewId())
			.userId(readModel.getUserId())
			.rating(readModel.getRating())
			.content(readModel.getContent())
			.createdAt(readModel.getCreatedAt())
			.rating(0) //Redis read model does not contain rating
			.build();
	}
}
