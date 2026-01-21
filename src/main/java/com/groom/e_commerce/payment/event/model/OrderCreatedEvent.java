package com.groom.e_commerce.payment.event.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCreatedEvent(
	UUID orderId,
	Long amount,
	LocalDateTime occurredAt
) {
	public static OrderCreatedEvent of(UUID orderId, Long amount) {
		return new OrderCreatedEvent(orderId, amount, LocalDateTime.now());
	}
}
