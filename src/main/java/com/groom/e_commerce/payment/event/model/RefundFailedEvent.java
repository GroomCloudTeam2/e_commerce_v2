package com.groom.e_commerce.payment.event.model;

import java.util.UUID;

public record RefundFailedEvent(
	UUID orderId,
	String paymentKey,
	Long cancelAmount,
	String failCode,
	String failMessage
) {
	public static RefundFailedEvent of(UUID orderId, String paymentKey, Long cancelAmount, String failCode, String failMessage) {
		return new RefundFailedEvent(orderId, paymentKey, cancelAmount, failCode, failMessage);
	}
}
