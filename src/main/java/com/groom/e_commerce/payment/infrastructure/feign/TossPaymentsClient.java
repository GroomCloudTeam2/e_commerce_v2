package com.groom.e_commerce.payment.infrastructure.feign;

import java.time.LocalDateTime;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.groom.e_commerce.payment.infrastructure.config.FeignConfig;

@FeignClient(
	name = "tossPaymentsClient",
	url = "${toss.payments.base-url}",
	configuration = FeignConfig.class
)
public interface TossPaymentsClient {

	@PostMapping("/v1/payments/confirm")
	TossConfirmResponse confirm(@RequestBody TossConfirmRequest request);

	@PostMapping("/v1/payments/{paymentKey}/cancel")
	TossCancelResponse cancel(
		@PathVariable("paymentKey") String paymentKey,
		@RequestBody TossCancelRequest request
	);

	// ===== DTOs =====

	record TossConfirmRequest(
		String paymentKey,
		String orderId,
		Long amount
	) {}

	record TossConfirmResponse(
		String paymentKey,
		String orderId,
		Long totalAmount,
		String status,
		@JsonProperty("approvedAt") String approvedAt // Toss가 ISO 문자열로 주는 경우가 많음
	) {
		public LocalDateTime approvedAtAsLocalDateTime() {
			if (approvedAt == null || approvedAt.isBlank()) return null;
			try {
				return LocalDateTime.parse(approvedAt);
			} catch (Exception e) {
				return null;
			}
		}
	}

	record TossCancelRequest(
		Long cancelAmount,
		String cancelReason
	) {}

	record TossCancelResponse(
		String paymentKey,
		String orderId,
		Long totalAmount,
		String status
	) {}
}
