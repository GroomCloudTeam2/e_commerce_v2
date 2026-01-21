package com.groom.e_commerce.payment.event.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record RefundSucceededEvent(
	UUID orderId,
	String paymentKey,
	Long cancelAmount,
	LocalDateTime occurredAt
) {
	public static RefundSucceededEvent of(UUID orderId, String paymentKey, Long cancelAmount) {
		return new RefundSucceededEvent(orderId, paymentKey, cancelAmount, LocalDateTime.now());
	}
}
