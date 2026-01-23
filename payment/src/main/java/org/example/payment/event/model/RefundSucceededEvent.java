package org.example.payment.event.model;

import java.util.UUID;

public record RefundSucceededEvent(
	UUID orderId,
	String paymentKey,
	Long cancelAmount
) {
	public static RefundSucceededEvent of(UUID orderId, String paymentKey, Long cancelAmount) {
		return new RefundSucceededEvent(orderId, paymentKey, cancelAmount);
	}
}
