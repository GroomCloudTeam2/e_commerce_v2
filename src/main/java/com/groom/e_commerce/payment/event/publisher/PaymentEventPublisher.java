package com.groom.e_commerce.payment.event.publisher;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.groom.e_commerce.payment.event.model.PaymentCompletedEvent;
import com.groom.e_commerce.payment.event.model.PaymentFailEvent;
import com.groom.e_commerce.payment.event.model.RefundFailEvent;
import com.groom.e_commerce.payment.event.model.RefundSucceededEvent;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

	private final ApplicationEventPublisher publisher;

	public void publishPaymentCompleted(UUID orderId, String paymentKey, Long amount) {
		publishAfterCommit(PaymentCompletedEvent.of(orderId, paymentKey, amount));
	}

	public void publishPaymentFailed(UUID orderId, String paymentKey, Long amount, String failCode, String failMessage) {
		publishAfterCommit(PaymentFailEvent.of(orderId, paymentKey, amount, failCode, failMessage));
	}

	public void publishRefundSucceeded(UUID orderId, String paymentKey, Long cancelAmount) {
		publishAfterCommit(RefundSucceededEvent.of(orderId, paymentKey, cancelAmount));
	}

	public void publishRefundFailed(UUID orderId, String paymentKey, Long cancelAmount, String failCode, String failMessage) {
		publishAfterCommit(RefundFailEvent.of(orderId, paymentKey, cancelAmount, failCode, failMessage));
	}

	private void publishAfterCommit(Object event) {
		// 트랜잭션 안이면 커밋 이후 발행
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					publisher.publishEvent(event);
				}
			});
			return;
		}

		// 트랜잭션 밖이면 즉시 발행
		publisher.publishEvent(event);
	}
}
