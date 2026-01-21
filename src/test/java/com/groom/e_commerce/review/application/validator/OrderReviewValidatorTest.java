package com.groom.e_commerce.review.application.validator;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.review.domain.repository.ReviewRepository;
import com.groom.e_commerce.review.infrastructure.feign.OrderClient;
import com.groom.e_commerce.review.infrastructure.feign.dto.OrderReviewValidationRequest;
import com.groom.e_commerce.review.infrastructure.feign.dto.OrderReviewValidationResponse;

@ExtendWith(MockitoExtension.class)
class OrderReviewValidatorTest {

	@Mock
	private OrderClient orderClient;

	@Mock
	private ReviewRepository reviewRepository;

	@InjectMocks
	private OrderReviewValidator validator;

	private UUID orderId;
	private UUID productId;
	private UUID userId;

	@BeforeEach
	void setUp() {
		orderId = UUID.randomUUID();
		productId = UUID.randomUUID();
		userId = UUID.randomUUID();
	}

	@Test
	@DisplayName("이미 리뷰가 존재하면 REVIEW_ALREADY_EXISTS 예외 발생")
	void alreadyReviewed() {
		// given
		given(reviewRepository.findByOrderIdAndProductId(orderId, productId))
			.willReturn(Optional.of(mock()));

		// when & then
		CustomException ex = catchThrowableOfType(
			() -> validator.validate(orderId, productId, userId),
			CustomException.class
		);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS);
		then(orderClient).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("주문이 존재하지 않으면 ORDER_NOT_FOUND 예외 발생")
	void orderNotFound() {
		// given
		given(reviewRepository.findByOrderIdAndProductId(orderId, productId))
			.willReturn(Optional.empty());

		given(orderClient.validateReviewOrder(any(OrderReviewValidationRequest.class)))
			.willReturn(new OrderReviewValidationResponse(
				false, false, false, null, false
			));

		// when & then
		CustomException ex = catchThrowableOfType(
			() -> validator.validate(orderId, productId, userId),
			CustomException.class
		);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
	}

	@Test
	@DisplayName("주문 소유자가 아니면 FORBIDDEN 예외 발생")
	void notOwner() {
		// given
		given(reviewRepository.findByOrderIdAndProductId(orderId, productId))
			.willReturn(Optional.empty());

		given(orderClient.validateReviewOrder(any()))
			.willReturn(new OrderReviewValidationResponse(
				true, false, true, "CONFIRMED", true
			));

		// when & then
		CustomException ex = catchThrowableOfType(
			() -> validator.validate(orderId, productId, userId),
			CustomException.class
		);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("주문에 상품이 포함되지 않으면 INVALID_REQUEST 예외 발생")
	void productNotInOrder() {
		// given
		given(reviewRepository.findByOrderIdAndProductId(orderId, productId))
			.willReturn(Optional.empty());

		given(orderClient.validateReviewOrder(any()))
			.willReturn(new OrderReviewValidationResponse(
				true, true, false, "CONFIRMED", true
			));

		// when & then
		CustomException ex = catchThrowableOfType(
			() -> validator.validate(orderId, productId, userId),
			CustomException.class
		);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("리뷰 가능한 주문 상태가 아니면 REVIEW_NOT_ALLOWED_ORDER_STATUS 예외 발생")
	void notReviewableStatus() {
		// given
		given(reviewRepository.findByOrderIdAndProductId(orderId, productId))
			.willReturn(Optional.empty());

		given(orderClient.validateReviewOrder(any()))
			.willReturn(new OrderReviewValidationResponse(
				true, true, true, "CANCELLED", false
			));

		// when & then
		CustomException ex = catchThrowableOfType(
			() -> validator.validate(orderId, productId, userId),
			CustomException.class
		);

		assertThat(ex.getErrorCode())
			.isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED_ORDER_STATUS);
	}

	@Test
	@DisplayName("모든 조건을 만족하면 예외 없이 통과")
	void success() {
		// given
		given(reviewRepository.findByOrderIdAndProductId(orderId, productId))
			.willReturn(Optional.empty());

		given(orderClient.validateReviewOrder(any()))
			.willReturn(new OrderReviewValidationResponse(
				true, true, true, "CONFIRMED", true
			));

		// when & then
		assertThatCode(() ->
			validator.validate(orderId, productId, userId)
		).doesNotThrowAnyException();
	}
}
