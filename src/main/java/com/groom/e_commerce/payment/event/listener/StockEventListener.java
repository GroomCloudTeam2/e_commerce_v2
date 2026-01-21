package com.groom.e_commerce.payment.event.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.groom.e_commerce.payment.application.service.PaymentCommandService;
import com.groom.e_commerce.payment.event.model.StockDeductionFailedEvent;
import com.groom.e_commerce.payment.presentation.dto.request.ReqCancelPayment;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StockEventListener {

	private final PaymentCommandService paymentCommandService;

	@EventListener
	public void handle(StockDeductionFailedEvent event) {
		// 재고 차감 실패 → 결제 환불(보상)
		paymentCommandService.cancel(new ReqCancelPayment(event.orderId(), event.reason()));
	}
}
