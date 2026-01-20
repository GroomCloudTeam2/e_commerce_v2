package com.groom.e_commerce.review.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.groom.e_commerce.global.infrastructure.client.Classification.AiRestClient;
import com.groom.e_commerce.review.domain.entity.ReviewCategory;
import com.groom.e_commerce.review.presentation.dto.request.CreateReviewRequest;

class ReviewServiceCreateTest extends ReviewServiceUnitTestSupport {

	@Test
	void 리뷰_생성_정상() {
		CreateReviewRequest request =
			new CreateReviewRequest(5, "디자인 좋음");

		when(productRatingRepository.findByProductId(productId))
			.thenReturn(Optional.empty());

		when(aiRestClient.classifyComment(anyString()))
			.thenReturn(new AiRestClient.AiResponse(
				ReviewCategory.DESIGN, 0.95
			));

		var response =
			reviewService.createReview(orderId, productId, userId, request);

		verify(orderReviewValidator)
			.validate(orderId, productId, userId);

		verify(reviewRepository).save(any());
		assertThat(response).isNotNull();
	}

	@Test
	void 리뷰_내용_50자_초과시_ERR_카테고리() {
		CreateReviewRequest request =
			new CreateReviewRequest(4, "a".repeat(60));

		when(productRatingRepository.findByProductId(productId))
			.thenReturn(Optional.empty());

		reviewService.createReview(orderId, productId, userId, request);

		verify(reviewRepository).save(
			argThat(r -> r.getCategory() == ReviewCategory.ERR)
		);
	}
}
