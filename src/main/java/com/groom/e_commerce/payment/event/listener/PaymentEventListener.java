package com.groom.e_commerce.payment.event.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.groom.e_commerce.order.domain.event.outbound.OrderCancelledEvent;
import com.groom.e_commerce.order.domain.event.outbound.OrderCreatedEvent;
import com.groom.e_commerce.payment.application.service.PaymentCommandService;
import com.groom.e_commerce.payment.presentation.dto.request.ReqCancelPayment;
import com.groom.e_commerce.product.application.event.dto.StockDeductionFailedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

	private final PaymentCommandService paymentCommandService;

	/**
	 * 주문 생성 이벤트 수신
	 * Order → Payment
	 */
	@EventListener
	public void handleOrderCreated(OrderCreatedEvent event) {
		log.info("[Payment] OrderCreatedEvent 수신 - orderId: {}, amount: {}",
				event.orderId(), event.amount());

		// 결제 READY 생성 (멱등)
		paymentCommandService.createReady(event.orderId(), event.amount());
	}

	/**
	 * 주문 취소 이벤트 수신
	 * Order → Payment
	 */
	@EventListener
	public void handleOrderCancelled(OrderCancelledEvent event) {
		log.info("[Payment] OrderCancelledEvent 수신 - orderId: {}, reason: {}",
				event.orderId(), event.reason());

		// 결제 취소 / 환불 트리거
		paymentCommandService.cancel(
				new ReqCancelPayment(event.orderId(), event.reason()));
	}

	/**
	 * 재고 차감 실패 이벤트 수신
	 * Product → Payment (보상 트랜잭션)
	 */
	@EventListener
	public void handleStockDeductionFailed(StockDeductionFailedEvent event) {
		log.warn("[Payment] StockDeductionFailedEvent 수신 - orderId: {}, reason: {}",
				event.getOrderId(), event.getFailReason());

		// 결제 취소(보상)
		paymentCommandService.cancel(
				new ReqCancelPayment(
						event.getOrderId(),
						event.getFailReason()));
	}
}
