package com.groom.e_commerce.review.infrastructure.feign;

import org.springframework.stereotype.Component;

import com.groom.e_commerce.review.infrastructure.feign.dto.OrderReviewValidationRequest;
import com.groom.e_commerce.review.infrastructure.feign.dto.OrderReviewValidationResponse;

@Component
public class OrderClientFallback implements OrderClient {

	@Override
	public OrderReviewValidationResponse validateReviewOrder(
		OrderReviewValidationRequest request
	) {
		// Order 검증 실패 = 리뷰 작성 불가
		return OrderReviewValidationResponse.fail();
	}
}
