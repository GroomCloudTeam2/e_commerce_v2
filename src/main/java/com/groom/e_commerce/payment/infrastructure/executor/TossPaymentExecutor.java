package com.groom.e_commerce.payment.infrastructure.executor;

import static com.groom.e_commerce.global.presentation.advice.ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE;

import org.springframework.stereotype.Component;

import com.groom.e_commerce.payment.infrastructure.feign.TossPaymentsClient;
import com.groom.e_commerce.payment.infrastructure.feign.TossPaymentsClient.TossCancelRequest;
import com.groom.e_commerce.payment.infrastructure.feign.TossPaymentsClient.TossCancelResponse;
import com.groom.e_commerce.payment.infrastructure.feign.TossPaymentsClient.TossConfirmRequest;
import com.groom.e_commerce.payment.infrastructure.feign.TossPaymentsClient.TossConfirmResponse;
import com.groom.e_commerce.payment.presentation.exception.PaymentException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentExecutor {

	private final TossPaymentsClient tossPaymentsClient;

	/**
	 * 결제 승인 요청 (CircuitBreaker 적용)
	 */
	@CircuitBreaker(name = "tossConfirm", fallbackMethod = "tossConfirmFallback")
	public TossConfirmResponse executeConfirm(TossConfirmRequest request) {
		return tossPaymentsClient.confirm(request);
	}

	public TossConfirmResponse tossConfirmFallback(TossConfirmRequest request, Throwable t) {
		// 외부 연동 장애(Timeout, 500, CB Open)를 도메인 예외로 변환
		throw new PaymentException(PAYMENT_GATEWAY_UNAVAILABLE,
			"Toss confirm unavailable. " + t.getClass().getSimpleName());
	}

	/**
	 * 결제 취소 요청 (CircuitBreaker 적용)
	 */
	@CircuitBreaker(name = "tossCancel", fallbackMethod = "tossCancelFallback")
	public TossCancelResponse executeCancel(String paymentKey, TossCancelRequest request) {
		return tossPaymentsClient.cancel(paymentKey, request);
	}

	public TossCancelResponse tossCancelFallback(String paymentKey, TossCancelRequest request, Throwable t) {
		log.error("Toss Cancel Fallback triggered. paymentKey={}, error={}", paymentKey, t.getMessage());
		throw new PaymentException(PAYMENT_GATEWAY_UNAVAILABLE,
			"Toss cancel unavailable. " + t.getClass().getSimpleName());
	}
}
