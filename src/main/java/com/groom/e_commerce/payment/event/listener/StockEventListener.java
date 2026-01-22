package com.groom.e_commerce.payment.event.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.groom.e_commerce.payment.application.service.PaymentCommandService;
import com.groom.e_commerce.payment.presentation.dto.request.ReqCancelPayment;
import com.groom.e_commerce.product.application.event.dto.StockDeductionFailedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventListener {

	private final PaymentCommandService paymentCommandService;

	@EventListener
	public void handleStockDeductionFailed(StockDeductionFailedEvent event) {
		// Product 재고 차감 실패 → Payment 결제 취소(보상)
		log.warn("[Payment] StockDeductionFailedEvent 수신 - orderId: {}, reason: {}",
			event.getOrderId(), event.getFailReason());

		// event는 record가 아니라 class(@Getter)라서 event.orderId() / event.reason() 불가
		paymentCommandService.cancel(
			new ReqCancelPayment(
				event.getOrderId(),
				event.getFailReason()
			)
		);
	}
}
