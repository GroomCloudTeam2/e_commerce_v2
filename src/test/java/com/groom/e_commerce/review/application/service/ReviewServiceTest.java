package com.groom.e_commerce.review.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.review.application.validator.OrderReviewValidator;
import com.groom.e_commerce.review.domain.repository.ReviewRepository;
import com.groom.e_commerce.review.infrastructure.feign.OrderClient;
import com.groom.e_commerce.review.infrastructure.feign.dto.OrderReviewValidationResponse;
import com.groom.e_commerce.review.presentation.dto.request.CreateReviewRequest;

@SpringBootTest
@Transactional
class ReviewServiceTest {

	@Autowired
	private ReviewService reviewService;

	@MockBean
	private OrderClient orderClient;

	@MockBean
	private ReviewRepository reviewRepository;

	@MockBean
	private OrderReviewValidator orderReviewValidator;

	@Test
	@DisplayName("리뷰 생성 성공 - validator 통과 시")
	void createReview_success() {
		UUID orderId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		CreateReviewRequest request =
			new CreateReviewRequest(5, "좋아요");

		// validator 통과
		willDoNothing()
			.given(orderReviewValidator)
			.validate(orderId, productId, userId);

		// when
		assertThatCode(() ->
			reviewService.createReview(orderId, productId, userId, request)
		).doesNotThrowAnyException();

		then(orderReviewValidator)
			.should()
			.validate(orderId, productId, userId);
	}

	@Test
	@DisplayName("리뷰 생성 실패 - 주문 검증 실패")
	void createReview_fail_validation() {
		UUID orderId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		CreateReviewRequest request =
			new CreateReviewRequest(5, "좋아요");

		willThrow(new CustomException(ErrorCode.ORDER_NOT_FOUND))
			.given(orderReviewValidator)
			.validate(orderId, productId, userId);

		CustomException ex = catchThrowableOfType(
			() -> reviewService.createReview(orderId, productId, userId, request),
			CustomException.class
		);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
	}
}
