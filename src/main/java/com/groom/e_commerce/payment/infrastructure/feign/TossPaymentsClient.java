package com.groom.e_commerce.payment.infrastructure.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // ✅ 필수 import
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@FeignClient(
	name = "tossPaymentsClient",
	url = "${toss.payments.base-url}"
)
public interface TossPaymentsClient {

	@PostMapping("/v1/payments/confirm")
	TossConfirmResponse confirm(@RequestBody TossConfirmRequest request);

	@PostMapping("/v1/payments/{paymentKey}/cancel")
	TossCancelResponse cancel(
		@PathVariable("paymentKey") String paymentKey,
		@RequestBody TossCancelRequest request
	);

	// ==========================================
	// 요청 객체 (Request): 직렬화 이슈 방지를 위해 Class 유지
	// ==========================================
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	class TossConfirmRequest {
		@JsonProperty("paymentKey")
		private String paymentKey;

		@JsonProperty("orderId")
		private String orderId;

		@JsonProperty("amount")
		private Long amount;
	}

	// ==========================================
	// 응답 객체 (Response): Record 사용 + 알 수 없는 필드 무시 설정
	// ==========================================

	@JsonIgnoreProperties(ignoreUnknown = true)
	record TossConfirmResponse(
		String paymentKey,
		String orderId,
		Long totalAmount,
		String status,
		@JsonProperty("approvedAt") String approvedAt
	) {}

	record TossCancelRequest(
		Long cancelAmount,
		String cancelReason
	) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record TossCancelResponse(
		String paymentKey,
		String orderId,
		Long totalAmount,
		String status
	) {}
}
