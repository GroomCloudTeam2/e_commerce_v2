package com.groom.e_commerce.product.application.event.publisher;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.groom.e_commerce.product.application.event.dto.StockDeductedEvent;
import com.groom.e_commerce.product.application.event.dto.StockDeductionFailedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Product 도메인 이벤트 발행자
 * 재고 처리 결과를 다른 도메인에 알립니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 재고 차감 성공 이벤트 발행
	 * Order 도메인에서 수신하여 주문 상태를 CONFIRMED로 변경
	 */
	public void publishStockDeducted(StockDeductedEvent event) {
		log.info("[Product] StockDeductedEvent 발행 - orderId: {}, items: {}",
			event.getOrderId(), event.getItems().size());
		eventPublisher.publishEvent(event);
	}

	/**
	 * 재고 차감 실패 이벤트 발행
	 * payment 도메인에서 수신하여 에러 처리 수행
	 */
	public void publishStockDeductionFailed(StockDeductionFailedEvent event) {
		log.error("[Product] StockDeductionFailedEvent 발행 - orderId: {}, reason: {}",
			event.getOrderId(), event.getFailReason());
		eventPublisher.publishEvent(event);
	}
}
