package org.example.payment.event.model;

import java.util.UUID;

public record PaymentFailEvent(
	UUID orderId,
	String paymentKey,
	Long amount,
	String failCode,
	String failMessage
) {
	public static PaymentFailEvent of(UUID orderId, String paymentKey, Long amount, String failCode, String failMessage) {
		return new PaymentFailEvent(orderId, paymentKey, amount, failCode, failMessage);
	}
}
