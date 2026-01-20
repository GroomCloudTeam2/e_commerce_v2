package com.groom.e_commerce.review.application.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.groom.e_commerce.review.domain.entity.ProductRatingEntity;
import com.groom.e_commerce.review.domain.entity.ReviewEntity;
import com.groom.e_commerce.user.domain.entity.user.UserRole;

class ReviewServiceDeleteTest extends ReviewServiceUnitTestSupport {

	@Test
	void 리뷰_삭제_본인() {
		ReviewEntity review = ReviewEntity.builder()
			.userId(userId)
			.productId(productId)
			.rating(5)
			.build();

		ProductRatingEntity rating =
			new ProductRatingEntity(productId);

		when(reviewRepository.findById(reviewId))
			.thenReturn(Optional.of(review));
		when(productRatingRepository.findByProductId(productId))
			.thenReturn(Optional.of(rating));

		reviewService.deleteReview(reviewId, userId, UserRole.USER);

		assertThat(review.getDeletedAt()).isNotNull();
	}

	@Test
	void 리뷰_삭제시_평점_감소() {
		ReviewEntity review = ReviewEntity.builder()
			.userId(userId)
			.productId(productId)
			.rating(5)
			.build();

		ProductRatingEntity rating =
			new ProductRatingEntity(productId);
		rating.updateRating(5);

		when(reviewRepository.findById(reviewId))
			.thenReturn(Optional.of(review));
		when(productRatingRepository.findByProductId(productId))
			.thenReturn(Optional.of(rating));

		reviewService.deleteReview(reviewId, userId, UserRole.USER);

		assertThat(rating.getReviewCount()).isEqualTo(0);
	}
}
