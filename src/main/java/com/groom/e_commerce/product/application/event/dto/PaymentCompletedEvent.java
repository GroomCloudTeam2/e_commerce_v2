package com.groom.e_commerce.product.application.event.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Payment → Product: 결제 완료 이벤트
 * Product는 이 이벤트를 수신하여 재고를 확정 차감합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {

	private UUID orderId;
	private UUID paymentId;
	private List<StockItem> items;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class StockItem {
		private UUID productId;
		private UUID variantId;
		private int quantity;
	}
}