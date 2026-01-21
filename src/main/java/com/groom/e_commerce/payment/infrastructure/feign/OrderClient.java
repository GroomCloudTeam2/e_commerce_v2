package com.groom.e_commerce.payment.infrastructure.feign;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.groom.e_commerce.payment.infrastructure.config.InternalFeignConfig;

@FeignClient(
	name = "orderClient",
	url = "${internal.order.base-url}",
	configuration = InternalFeignConfig.class
)
public interface OrderClient {

	@GetMapping("/internal/orders/{orderId}")
	OrderSummaryResponse getOrder(@PathVariable("orderId") UUID orderId);

	record OrderSummaryResponse(
		UUID orderId,
		Long totalAmount,
		String status
	) {}
}
