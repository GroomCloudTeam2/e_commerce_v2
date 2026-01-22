package com.groom.e_commerce.payment.event.model;

import java.util.UUID;

public record RefundFailEvent(
	UUID orderId,
	String paymentKey,
	Long cancelAmount,
	String failCode,
	String failMessage
) {
	public static RefundFailEvent of(UUID orderId, String paymentKey, Long cancelAmount, String failCode, String failMessage) {
		return new RefundFailEvent(orderId, paymentKey, cancelAmount, failCode, failMessage);
	}
}
