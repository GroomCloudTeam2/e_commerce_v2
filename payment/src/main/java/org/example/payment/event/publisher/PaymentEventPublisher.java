package org.example.payment.event.publisher;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.groom.e_commerce.payment.event.model.PaymentCompletedEvent;
import com.groom.e_commerce.payment.event.model.PaymentFailEvent;
import com.groom.e_commerce.payment.event.model.RefundFailEvent;
import com.groom.e_commerce.payment.event.model.RefundSucceededEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

	private final ApplicationEventPublisher publisher;

	public void publishPaymentCompleted(UUID orderId, String paymentKey, Long amount) {
		log.info("[PaymentEvent] PaymentCompletedEvent 발행 요청 - orderId={}, amount={}",
			orderId, amount);

		publishAfterCommit(PaymentCompletedEvent.of(orderId, paymentKey, amount));
	}

	public void publishPaymentFailed(UUID orderId, String paymentKey, Long amount, String failCode, String failMessage) {
		log.warn("[PaymentEvent] PaymentFailEvent 발행 요청 - orderId={}, amount={}, failCode={}, message={}",
			orderId, amount, failCode, failMessage);

		publishAfterCommit(PaymentFailEvent.of(orderId, paymentKey, amount, failCode, failMessage));
	}

	public void publishRefundSucceeded(UUID orderId, String paymentKey, Long cancelAmount) {
		log.info("[PaymentEvent] RefundSucceededEvent 발행 요청 - orderId={}, cancelAmount={}",
			orderId, cancelAmount);

		publishAfterCommit(RefundSucceededEvent.of(orderId, paymentKey, cancelAmount));
	}

	public void publishRefundFailed(UUID orderId, String paymentKey, Long cancelAmount, String failCode, String failMessage) {
		log.error("[PaymentEvent] RefundFailEvent 발행 요청 - orderId={}, cancelAmount={}, failCode={}, message={}",
			orderId, cancelAmount, failCode, failMessage);

		publishAfterCommit(RefundFailEvent.of(orderId, paymentKey, cancelAmount, failCode, failMessage));
	}

	private void publishAfterCommit(Object event) {
		// 트랜잭션 안이면 커밋 이후 발행
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			log.debug("[PaymentEvent] 트랜잭션 활성 상태 → afterCommit 발행 예약. event={}",
				event.getClass().getSimpleName());

			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					log.debug("[PaymentEvent] afterCommit 발행 실행. event={}",
						event.getClass().getSimpleName());
					publisher.publishEvent(event);
				}
			});
			return;
		}

		// 트랜잭션 밖이면 즉시 발행
		log.debug("[PaymentEvent] 트랜잭션 없음 → 즉시 발행. event={}",
			event.getClass().getSimpleName());
		publisher.publishEvent(event);
	}
}
