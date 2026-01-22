package com.groom.e_commerce.payment.event.model;

import java.util.UUID;

public record PaymentFailedEvent(
	UUID orderId,
	String paymentKey,
	Long amount,
	String failCode,
	String failMessage
) {
	public static PaymentFailedEvent of(UUID orderId, String paymentKey, Long amount, String failCode, String failMessage) {
		return new PaymentFailedEvent(orderId, paymentKey, amount, failCode, failMessage);
	}
}
