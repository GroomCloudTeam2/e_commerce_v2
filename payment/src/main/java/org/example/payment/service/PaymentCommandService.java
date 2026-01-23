package org.example.payment.service;

import static com.groom.e_commerce.global.presentation.advice.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.entity.PaymentCancel;
import com.groom.e_commerce.payment.domain.model.PaymentStatus;
import com.groom.e_commerce.payment.domain.repository.PaymentCancelRepository;
import com.groom.e_commerce.payment.domain.repository.PaymentRepository;
import com.groom.e_commerce.payment.event.publisher.PaymentEventPublisher;
import com.groom.e_commerce.payment.infrastructure.feign.TossPaymentsClient;
import com.groom.e_commerce.payment.infrastructure.feign.TossPaymentsClient.TossCancelRequest;
import com.groom.e_commerce.payment.infrastructure.feign.TossPaymentsClient.TossCancelResponse;
import com.groom.e_commerce.payment.infrastructure.feign.TossPaymentsClient.TossConfirmRequest;
import com.groom.e_commerce.payment.infrastructure.feign.TossPaymentsClient.TossConfirmResponse;
import com.groom.e_commerce.payment.presentation.dto.request.ReqCancelPayment;
import com.groom.e_commerce.payment.presentation.dto.request.ReqConfirmPayment;
import com.groom.e_commerce.payment.presentation.dto.response.ResCancelResult;
import com.groom.e_commerce.payment.presentation.dto.response.ResPayment;
import com.groom.e_commerce.payment.presentation.exception.PaymentException;
import com.groom.e_commerce.payment.presentation.exception.TossApiException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentCommandService {
	private static final String PG_TOSS = "TOSS";

	private final PaymentRepository paymentRepository;
	private final PaymentCancelRepository paymentCancelRepository;

	private final TossPaymentsClient tossPaymentsClient;
	private final PaymentEventPublisher paymentEventPublisher;

	/**
	 * READY 생성 (OrderCreatedEvent 소비 전용)
	 *
	 * 목적
	 * - 주문이 생성되면(PENDING) 결제 도메인에 "결제 준비 레코드"를 미리 만들어 둠
	 * - amount는 이 시점에 확정/고정(결제 승인 시 요청 amount를 믿지 않음)
	 *
	 * 멱등성
	 * - orderId 기준으로 이미 Payment가 있으면 아무 것도 하지 않음(no-op)
	 *   (이벤트 중복 발행/중복 소비 상황에서 중복 insert 방지)
	 */
	@Transactional
	public void createReady(UUID orderId, Long amount) {
		paymentRepository.findByOrderId(orderId)
			.ifPresentOrElse(
				existing -> {
					// 멱등: 이미 있으면 아무것도 안 함
					// - 이벤트 중복 소비/재처리 상황에서 안전하게 무시
				},
				() -> paymentRepository.save(Payment.ready(orderId, amount, PG_TOSS))
			);
	}

	/**
	 * 결제 승인(confirm)
	 *
	 * 흐름
	 * 1) orderId로 Payment 조회 (READY가 선행되어야 함)
	 * 2) 상태 기반 멱등/가드:
	 *    - PAID면 기존 결과 반환(재호출 안전)
	 *    - CANCELLED/FAILED/READY 아님이면 예외
	 * 3) Toss confirm 호출(Feign + CircuitBreaker)
	 * 4) Toss 응답이 DONE이면:
	 *    - PaymentStatus=PAID로 전이
	 *    - PaymentCompletedEvent 발행(After Commit)
	 * 5) Toss confirm 실패/비정상 상태면:
	 *    - PaymentStatus=FAILED로 전이
	 *    - PaymentFailedEvent 발행(After Commit)
	 *
	 * 주의
	 * - amount는 요청 값(req.amount)이 아니라, READY 때 저장해둔 payment.getAmount()를 사용
	 *   (클라이언트 조작/불일치 방지 + 도메인에서 금액을 "단일 소스"로 유지)
	 */
	@Transactional
	public ResPayment confirm(ReqConfirmPayment req) {
		UUID orderId = req.orderId();

		// 1) READY로 생성된 Payment가 있어야 승인 가능
		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new PaymentException(PAYMENT_NOT_FOUND, "Payment not found. orderId=" + orderId));

		// 2) 상태 기반 멱등/가드
		if (payment.getStatus() == PaymentStatus.PAID) {
			// 이미 승인 완료면 동일 결과 반환(중복 호출 안전)
			return ResPayment.from(payment);
		}
		if (payment.getStatus() == PaymentStatus.CANCELLED) {
			// 이미 취소된 결제는 승인 불가
			throw new PaymentException(PAYMENT_ALREADY_CANCELLED, "Payment already cancelled.");
		}
		if (payment.getStatus() == PaymentStatus.FAILED) {
			// 이미 실패 처리된 결제는 승인 불가(정책)
			throw new PaymentException(PAYMENT_ALREADY_FAILED, "Payment already failed.");
		}
		if (payment.getStatus() != PaymentStatus.READY) {
			// READY가 아닌 상태(예: UNKNOWN 등)에서는 승인 불가
			throw new PaymentException(PAYMENT_NOT_CONFIRMABLE, "Payment not confirmable. status=" + payment.getStatus());
		}

		// 3) 승인 요청 금액은 Payment에 저장된 확정 금액 사용
		Long amount = payment.getAmount();

		// 4) Toss confirm 호출
		TossConfirmResponse tossRes;
		try {
			tossRes = tossConfirmWithCb(new TossConfirmRequest(
				req.paymentKey(),
				orderId.toString(),
				amount
			));
		} catch (TossApiException e) {
			// Toss 측에서 "정상적으로 실패 응답"을 내려준 케이스(4xx/정책 거절 등)
			String failCode = e.getErrorCode().getCode();
			String failMessage = e.getErrorCode().getMessage();

			// 도메인 상태를 FAILED로 기록
			payment.markFailed(failCode, failMessage);
			paymentRepository.save(payment);

			// 후속 도메인(Order/Product 등)에 실패 이벤트 전파
			paymentEventPublisher.publishPaymentFailed(orderId, req.paymentKey(), amount, failCode, failMessage);

			// 컨트롤러까지 예외 전달(글로벌 핸들러가 ErrorResponse 생성)
			throw e;
		} catch (Exception e) {
			// 네트워크 장애/타임아웃/예상치 못한 예외
			// MVP 정책: UNKNOWN 실패로 일단 FAILED 처리 + 실패 이벤트 발행
			payment.markFailed("TOSS_CONFIRM_UNKNOWN", e.getMessage());
			paymentRepository.save(payment);

			paymentEventPublisher.publishPaymentFailed(orderId, req.paymentKey(), amount, "TOSS_CONFIRM_UNKNOWN", e.getMessage());

			throw new PaymentException(PAYMENT_CONFIRM_ERROR, "Confirm failed (unknown). " + e.getMessage());
		}

		// 5) Toss 응답 상태 확인
		String tossStatus = safe(tossRes.status());

		if ("DONE".equalsIgnoreCase(tossStatus)) {
			// 승인 성공(DONE) → PAID 전이 + 완료 이벤트 발행
			payment.markPaid(req.paymentKey(), amount);
			paymentRepository.save(payment);

			paymentEventPublisher.publishPaymentCompleted(orderId, req.paymentKey(), amount);
			return ResPayment.from(payment);
		}

		// DONE이 아닌데 2xx로 내려온 경우 → 정책상 실패 처리
		payment.markFailed("TOSS_NOT_DONE", "Toss status=" + tossStatus);
		paymentRepository.save(payment);

		paymentEventPublisher.publishPaymentFailed(orderId, req.paymentKey(), amount, "TOSS_NOT_DONE", "status=" + tossStatus);
		throw new PaymentException(PAYMENT_NOT_DONE, "Toss payment status is not DONE. status=" + tossStatus);
	}

	/**
	 * 결제 취소(환불)
	 *
	 * 트리거
	 * - OrderCancelledEvent (사용자 주문 취소)
	 * - StockDeductionFailedEvent (재고 확정 차감 실패에 대한 보상 트랜잭션)
	 * - (운영/테스트용) 수동 cancel API
	 *
	 * 동작
	 * 1) Payment 조회
	 * 2) 멱등:
	 *    - 이미 CANCELLED면 성공 처리로 반환(중복 이벤트/재시도 안전)
	 * 3) 환불 대상 체크:
	 *    - PAID가 아니면 환불 호출 없이 종료(정책상 no-op)
	 * 4) Toss cancel 호출(Feign + CircuitBreaker)
	 * 5) 성공(CANCELED/PARTIAL_CANCELED):
	 *    - PaymentCancel 기록
	 *    - PaymentStatus=CANCELLED 전이
	 *    - RefundSucceededEvent 발행
	 * 6) 실패:
	 *    - Payment는 PAID 유지(도메인 정책)
	 *    - RefundFailedEvent 발행
	 */
	@Transactional
	public ResCancelResult cancel(ReqCancelPayment req) {
		UUID orderId = req.orderId();

		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new PaymentException(PAYMENT_NOT_FOUND, "Payment not found. orderId=" + orderId));

		// 1) 멱등 처리: 이미 취소되어 있으면 그대로 성공 반환
		if (payment.getStatus() == PaymentStatus.CANCELLED) {
			return ResCancelResult.from(payment, true, "ALREADY_CANCELLED");
		}

		// 2) 환불 대상 아님: PAID가 아니면 PG 호출하지 않고 종료
		if (payment.getStatus() != PaymentStatus.PAID) {
			return ResCancelResult.from(payment, false, "NOT_REFUNDABLE_STATUS=" + payment.getStatus());
		}

		// 3) PAID인데 paymentKey가 없으면 데이터 이상(내부 오류)
		String paymentKey = payment.getPaymentKey();
		if (paymentKey == null || paymentKey.isBlank()) {
			throw new PaymentException(PAYMENT_KEY_MISSING, "paymentKey missing for PAID payment.");
		}

		// 4) 취소 금액/사유 구성
		Long cancelAmount = payment.getAmount();
		String cancelReason = (req.cancelReason() == null || req.cancelReason().isBlank())
			? "ORDER_CANCELLED"
			: req.cancelReason();

		// 5) Toss cancel 호출
		TossCancelResponse tossRes;
		try {
			tossRes = tossCancelWithCb(paymentKey, new TossCancelRequest(cancelAmount, cancelReason));
		} catch (TossApiException e) {
			// Toss가 명시적으로 실패 응답
			String failCode = e.getErrorCode().getCode();
			String failMessage = e.getErrorCode().getMessage();

			// 실패 기록(상태는 PAID 유지 + 실패 사유만 기록)
			payment.markRefundFailed(failCode, failMessage);
			paymentRepository.save(payment);

			// 환불 실패 이벤트 발행
			paymentEventPublisher.publishRefundFailed(orderId, paymentKey, cancelAmount, failCode, failMessage);
			return ResCancelResult.from(payment, false, "REFUND_FAILED:" + failCode);
		} catch (Exception e) {
			// 네트워크/타임아웃/CB Open 등
			String failCode = "TOSS_CANCEL_UNKNOWN";
			String failMessage = (e.getMessage() == null) ? "unknown" : e.getMessage();

			payment.markRefundFailed(failCode, failMessage);
			paymentRepository.save(payment);

			paymentEventPublisher.publishRefundFailed(orderId, paymentKey, cancelAmount, failCode, failMessage);
			return ResCancelResult.from(payment, false, "REFUND_FAILED:" + failCode);
		}

		// 6) Toss 응답 상태 확인
		String tossStatus = safe(tossRes.status());

		// 환불 성공
		if ("CANCELED".equalsIgnoreCase(tossStatus) || "PARTIAL_CANCELED".equalsIgnoreCase(tossStatus)) {
			// 취소 이력 저장
			PaymentCancel cancel = PaymentCancel.of(
				payment.getPaymentId(),
				paymentKey,
				cancelAmount,
				LocalDateTime.now()
			);
			paymentCancelRepository.save(cancel);

			// 결제 상태 CANCELLED 전이
			payment.markCancelled();
			paymentRepository.save(payment);

			// 환불 성공 이벤트 발행
			paymentEventPublisher.publishRefundSucceeded(orderId, paymentKey, cancelAmount);
			return ResCancelResult.from(payment, true, "REFUND_SUCCEEDED");
		}

		// 예상 밖 상태 → 실패 취급(상태는 PAID 유지)
		String failCode = "TOSS_CANCEL_NOT_CANCELED";
		String failMessage = "Toss status=" + tossStatus;

		payment.markRefundFailed(failCode, failMessage);
		paymentRepository.save(payment);

		paymentEventPublisher.publishRefundFailed(orderId, paymentKey, cancelAmount, failCode, failMessage);
		return ResCancelResult.from(payment, false, "REFUND_FAILED:" + failCode);
	}

	/**
	 * null-safe 문자열
	 */
	private String safe(String v) {
		return v == null ? "" : v;
	}

	/* =========================
	 * Resilience4j CircuitBreaker 적용 메서드
	 * ========================= */

	/**
	 * Toss confirm 호출 경계
	 * - CircuitBreaker 적용 대상은 "외부 호출"이 일어나는 public 메서드가 가장 안전
	 */
	@CircuitBreaker(name = "tossConfirm")
	public TossConfirmResponse tossConfirmWithCb(TossConfirmRequest request) {
		return tossPaymentsClient.confirm(request);
	}

	/**
	 * Toss cancel 호출 경계
	 * - CircuitBreaker 적용 대상(외부 호출)
	 */
	@CircuitBreaker(name = "tossCancel")
	public TossCancelResponse tossCancelWithCb(String paymentKey, TossCancelRequest request) {
		return tossPaymentsClient.cancel(paymentKey, request);
	}
}
