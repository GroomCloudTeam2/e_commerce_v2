package com.groom.e_commerce.review.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.groom.e_commerce.review.domain.entity.ReviewCategory;
import com.groom.e_commerce.review.domain.entity.ReviewEntity;
import com.groom.e_commerce.review.domain.entity.ReviewLikeEntity;

class ReviewServiceLikeTest extends ReviewServiceUnitTestSupport {

	@Test
	void 좋아요_성공() {
		ReviewEntity review = ReviewEntity.builder()
			.orderId(orderId)
			.productId(productId)
			.userId(userId)
			.rating(5)
			.content("좋아요")
			.category(ReviewCategory.DESIGN)
			.build();

		when(reviewRepository.findById(reviewId))
			.thenReturn(Optional.of(review));

		when(reviewLikeRepository.findByReviewIdAndUserId(reviewId, userId))
			.thenReturn(Optional.empty());

		int count = reviewService.likeReview(reviewId, userId);

		assertThat(count).isEqualTo(1);
	}


	@Test
	void 좋아요_중복_실패() {
		ReviewEntity review = ReviewEntity.builder()
			.orderId(orderId)
			.productId(productId)
			.userId(userId)
			.rating(5)
			.content("좋아요")
			.category(ReviewCategory.DESIGN)
			.build();

		when(reviewRepository.findById(reviewId))
			.thenReturn(Optional.of(review));

		when(reviewLikeRepository.findByReviewIdAndUserId(reviewId, userId))
			.thenReturn(Optional.of(new ReviewLikeEntity(reviewId, userId)));

		assertThatThrownBy(() ->
			reviewService.likeReview(reviewId, userId)
		).isInstanceOf(IllegalStateException.class);
	}
}
