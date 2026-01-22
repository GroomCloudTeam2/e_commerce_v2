package com.groom.e_commerce.payment.event.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.groom.e_commerce.order.domain.event.outbound.OrderCancelledEvent;
import com.groom.e_commerce.order.domain.event.outbound.OrderCreatedEvent;
import com.groom.e_commerce.payment.application.service.PaymentCommandService;
import com.groom.e_commerce.payment.presentation.dto.request.ReqCancelPayment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

	private final PaymentCommandService paymentCommandService;

	@EventListener
	public void handleOrderCreated(OrderCreatedEvent event) {
		// 주문 생성 → Payment READY 생성 (이벤트 전용)
		log.info("[Payment] OrderCreatedEvent 수신 - orderId: {}, amount: {}",
			event.orderId(), event.amount());

		paymentCommandService.createReady(event.orderId(), event.amount());
	}

	@EventListener
	public void handleOrderCancelled(OrderCancelledEvent event) {
		// 주문 취소 → 결제 취소/환불 트리거
		log.info("[Payment] OrderCancelledEvent 수신 - orderId: {}, reason: {}",
			event.orderId(), event.reason());

		paymentCommandService.cancel(
			new ReqCancelPayment(event.orderId(), event.reason())
		);
	}
}
