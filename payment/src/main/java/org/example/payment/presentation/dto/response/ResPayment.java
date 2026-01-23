package org.example.payment.presentation.dto.response;

import java.util.UUID;

import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.model.PaymentStatus;

public record ResPayment(
	UUID paymentId,
	UUID orderId,
	String paymentKey,
	Long amount,
	PaymentStatus status
) {
	public static ResPayment from(Payment payment) {
		return new ResPayment(
			payment.getPaymentId(),
			payment.getOrderId(),
			payment.getPaymentKey(),
			payment.getAmount(),
			payment.getStatus()

		);
	}
}
