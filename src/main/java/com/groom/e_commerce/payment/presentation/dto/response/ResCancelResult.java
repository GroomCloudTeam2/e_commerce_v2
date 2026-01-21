package com.groom.e_commerce.payment.presentation.dto.response;

import java.util.UUID;

import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.model.PaymentStatus;

public record ResCancelResult(
	UUID paymentId,
	UUID orderId,
	String paymentKey,
	Long cancelAmount,
	boolean cancelled,
	PaymentStatus status,
	String message
) {
	public static ResCancelResult from(Payment payment, boolean cancelled, String message) {
		return new ResCancelResult(
			payment.getPaymentId(),
			payment.getOrderId(),
			payment.getPaymentKey(),
			payment.getAmount(),
			cancelled,
			payment.getStatus(),
			message
		);
	}
}
