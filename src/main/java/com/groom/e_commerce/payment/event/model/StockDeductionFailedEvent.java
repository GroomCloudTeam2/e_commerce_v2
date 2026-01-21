package com.groom.e_commerce.payment.event.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockDeductionFailedEvent(
	UUID orderId,
	String reason,
	LocalDateTime occurredAt
) {
	public static StockDeductionFailedEvent of(UUID orderId, String reason) {
		return new StockDeductionFailedEvent(orderId, reason, LocalDateTime.now());
	}
}
