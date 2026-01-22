package com.groom.e_commerce.payment.event.model;

import java.util.UUID;

public record PaymentCompletedEvent(
	UUID orderId,
	String paymentKey,
	Long amount
) {
	public static PaymentCompletedEvent of(UUID orderId, String paymentKey, Long amount) {
		return new PaymentCompletedEvent(orderId, paymentKey, amount);
	}
}
