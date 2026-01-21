package com.groom.e_commerce.product.application.event.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Product → Order: 재고 차감 실패 이벤트
 * Order는 이 이벤트를 수신하여 적절한 에러 처리를 수행합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDeductionFailedEvent {

	private UUID orderId;
	private String failReason;
	private List<FailedItem> failedItems;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class FailedItem {
		private UUID productId;
		private UUID variantId;
		private int requestedQuantity;
		private int availableStock;
		private String reason;
	}
}