package com.groom.e_commerce.review.application.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.groom.e_commerce.global.infrastructure.client.Classification.AiRestClient;
import com.groom.e_commerce.review.domain.entity.ProductRatingEntity;
import com.groom.e_commerce.review.domain.entity.ReviewCategory;
import com.groom.e_commerce.review.domain.entity.ReviewEntity;
import com.groom.e_commerce.review.presentation.dto.request.UpdateReviewRequest;

class ReviewServiceUpdateTest extends ReviewServiceUnitTestSupport {

	@Test
	void 리뷰_수정_본인_성공() {
		ReviewEntity review = ReviewEntity.builder()
			.userId(userId)
			.productId(productId)
			.rating(3)
			.build();

		when(reviewRepository.findById(reviewId))
			.thenReturn(Optional.of(review));

		when(productRatingRepository.findByProductId(productId))
			.thenReturn(Optional.of(new ProductRatingEntity(productId)));

		when(aiRestClient.classifyComment(anyString()))
			.thenReturn(new AiRestClient.AiResponse(
				ReviewCategory.QUALITY, 0.8
			));

		reviewService.updateReview(
			reviewId, userId, new UpdateReviewRequest("수정", 5)
		);

		assertThat(review.getRating()).isEqualTo(5);
		assertThat(review.getCategory()).isEqualTo(ReviewCategory.QUALITY);
	}

	@Test
	void 리뷰_수정_권한없음() {
		ReviewEntity review = ReviewEntity.builder()
			.userId(UUID.randomUUID())
			.build();

		when(reviewRepository.findById(reviewId))
			.thenReturn(Optional.of(review));

		assertThatThrownBy(() ->
			reviewService.updateReview(
				reviewId, userId, new UpdateReviewRequest("x", 1)
			)
		).isInstanceOf(SecurityException.class);
	}
}
