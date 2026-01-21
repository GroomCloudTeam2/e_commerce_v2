package com.groom.e_commerce.product.application.event.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Product → Order: 재고 차감 성공 이벤트
 * Order는 이 이벤트를 수신하여 주문 상태를 CONFIRMED로 변경합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDeductedEvent {

	private UUID orderId;
	private List<DeductedItem> items;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DeductedItem {
		private UUID productId;
		private UUID variantId;
		private int quantity;
		private int remainingStock;
	}
}