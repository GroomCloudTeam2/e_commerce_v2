package com.groom.e_commerce.product.application.event.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Payment → Product: 결제 실패 이벤트
 * Product는 이 이벤트를 수신하여 가점유된 재고를 복구합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailEvent {

	private UUID orderId;
	private UUID paymentId;
	private String failReason;
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