package com.groom.e_commerce.payment.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groom.e_commerce.payment.application.service.PaymentCommandService;
import com.groom.e_commerce.payment.presentation.dto.request.ReqCancelPayment;
import com.groom.e_commerce.payment.presentation.dto.request.ReqConfirmPayment;
import com.groom.e_commerce.payment.presentation.dto.response.ResCancelResult;
import com.groom.e_commerce.payment.presentation.dto.response.ResPayment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentControllerV1 {

	private final PaymentCommandService paymentCommandService;

	/**
	 * 결제 승인
	 * - Client가 결제 성공 후 호출
	 * - PaymentStatus 기반 멱등 처리
	 */
	@PostMapping("/confirm")
	public ResponseEntity<ResPayment> confirm(@Valid @RequestBody ReqConfirmPayment req) {
		return ResponseEntity.ok(paymentCommandService.confirm(req));
	}

	/**
	 * 결제 취소(환불)
	 * - 주로 이벤트(OrderCancelledEvent / StockDeductionFailedEvent)로 호출
	 * - 운영/수동 테스트를 위해 API로도 제공
	 */
	@PostMapping("/cancel")
	public ResponseEntity<ResCancelResult> cancel(@Valid @RequestBody ReqCancelPayment req) {
		return ResponseEntity.ok(paymentCommandService.cancel(req));
	}
}
