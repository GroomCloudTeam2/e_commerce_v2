package com.groom.e_commerce.payment.event.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.groom.e_commerce.payment.application.service.PaymentCommandService;
import com.groom.e_commerce.payment.event.model.OrderCancelledEvent;
import com.groom.e_commerce.payment.event.model.OrderCreatedEvent;
import com.groom.e_commerce.payment.presentation.dto.request.ReqCancelPayment;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

	private final PaymentCommandService paymentCommandService;

	@EventListener
	public void handle(OrderCreatedEvent event) {
		// 주문 생성 → Payment READY 생성 (이벤트 전용)
		paymentCommandService.createReady(event.orderId(), event.amount());
	}

	@EventListener
	public void handle(OrderCancelledEvent event) {
		// 주문 취소 → 결제 환불 트리거
		paymentCommandService.cancel(new ReqCancelPayment(event.orderId(), event.reason()));
	}
}
