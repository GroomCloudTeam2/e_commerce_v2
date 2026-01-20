package com.groom.e_commerce.review.infrastructure.feign;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.groom.e_commerce.cart.infrastructure.feign.ProductClientFallback;
import com.groom.e_commerce.global.infrastructure.feign.config.FeignConfig;
import com.groom.e_commerce.review.infrastructure.feign.dto.OrderReviewValidationRequest;
import com.groom.e_commerce.review.infrastructure.feign.dto.OrderReviewValidationResponse;

@FeignClient(
	name = "order-service",
	url = "${external.order-service.url}",
	configuration = FeignConfig.class,
	fallback = ProductClientFallback.class
)
public interface OrderClient {

	@PostMapping("/internal/api/v1/review/isReviewable")
	OrderReviewValidationResponse validateReviewOrder(
		@RequestBody OrderReviewValidationRequest request
	);
}
