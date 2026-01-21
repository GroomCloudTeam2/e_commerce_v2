package com.groom.e_commerce.payment.event.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCancelledEvent(
	UUID orderId,
	String reason,
	LocalDateTime occurredAt
) {
	public static OrderCancelledEvent of(UUID orderId, String reason) {
		return new OrderCancelledEvent(orderId, reason, LocalDateTime.now());
	}
}
