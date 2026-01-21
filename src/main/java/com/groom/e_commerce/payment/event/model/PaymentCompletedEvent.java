package com.groom.e_commerce.payment.event.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCompletedEvent(
	UUID orderId,
	String paymentKey,
	Long amount,
	LocalDateTime occurredAt
) {
	public static PaymentCompletedEvent of(UUID orderId, String paymentKey, Long amount) {
		return new PaymentCompletedEvent(orderId, paymentKey, amount, LocalDateTime.now());
	}
}
