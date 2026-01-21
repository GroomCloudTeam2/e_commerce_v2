package com.groom.e_commerce.payment.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.groom.e_commerce.payment.domain.model.PaymentStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "p_payment",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_payment_order_id", columnNames = "order_id"),
		@UniqueConstraint(name = "uk_payment_payment_key", columnNames = "payment_key")
	},
	indexes = {
		@Index(name = "ix_payment_order_id", columnList = "order_id"),
		@Index(name = "ix_payment_payment_key", columnList = "payment_key"),
		@Index(name = "ix_payment_status", columnList = "status")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

	@Id
	@Column(name = "payment_id", columnDefinition = "uuid")
	private UUID paymentId;

	@Column(name = "order_id", nullable = false, columnDefinition = "uuid")
	private UUID orderId;

	@Column(name = "amount", nullable = false)
	private Long amount;

	@Column(name = "payment_key", length = 200)
	private String paymentKey;

	@Column(name = "pg_provider", length = 50, nullable = false)
	private String pgProvider; // ex) "TOSS"

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 30, nullable = false)
	private PaymentStatus status;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	// 승인 실패 사유(선택)
	@Column(name = "fail_code", length = 100)
	private String failCode;

	@Column(name = "fail_message", length = 500)
	private String failMessage;

	// 환불 실패 사유(선택) - 상태는 PAID 유지 정책을 위해
	@Column(name = "refund_fail_code", length = 100)
	private String refundFailCode;

	@Column(name = "refund_fail_message", length = 500)
	private String refundFailMessage;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = this.createdAt;
		if (this.paymentId == null) this.paymentId = UUID.randomUUID();
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	private Payment(UUID orderId, Long amount, String pgProvider) {
		this.paymentId = UUID.randomUUID();
		this.orderId = orderId;
		this.amount = amount;
		this.pgProvider = pgProvider;
		this.status = PaymentStatus.READY;
		this.createdAt = LocalDateTime.now();
		this.updatedAt = this.createdAt;
	}

	public static Payment ready(UUID orderId, Long amount, String pgProvider) {
		if (orderId == null) throw new IllegalArgumentException("orderId is null");
		if (amount == null || amount <= 0) throw new IllegalArgumentException("amount is invalid");
		if (pgProvider == null || pgProvider.isBlank()) throw new IllegalArgumentException("pgProvider is blank");
		return new Payment(orderId, amount, pgProvider);
	}

	public boolean isConfirmable() {
		return this.status == PaymentStatus.READY;
	}

	public boolean isRefundable() {
		return this.status == PaymentStatus.PAID;
	}

	/**
	 * Toss confirm 성공(DONE) 확정 처리
	 */
	public void markPaid(String paymentKey, Long approvedAmount, LocalDateTime approvedAt) {
		if (!isConfirmable()) {
			throw new IllegalStateException("Payment is not confirmable. status=" + status);
		}
		if (paymentKey == null || paymentKey.isBlank()) {
			throw new IllegalArgumentException("paymentKey is blank");
		}
		if (approvedAmount == null || approvedAmount <= 0) {
			throw new IllegalArgumentException("approvedAmount is invalid");
		}
		// 금액 확정(검증은 서비스에서 Order 금액과 비교)
		this.paymentKey = paymentKey;
		this.amount = approvedAmount;
		this.approvedAt = approvedAt != null ? approvedAt : LocalDateTime.now();
		this.status = PaymentStatus.PAID;

		// 성공 시 실패 정보 초기화
		this.failCode = null;
		this.failMessage = null;
		this.refundFailCode = null;
		this.refundFailMessage = null;
	}

	/**
	 * Toss confirm 실패(ABORTED/EXPIRED 등)
	 */
	public void markFailed(String failCode, String failMessage) {
		if (this.status == PaymentStatus.PAID || this.status == PaymentStatus.CANCELLED) {
			throw new IllegalStateException("Already finalized payment. status=" + status);
		}
		this.status = PaymentStatus.FAILED;
		this.failCode = failCode;
		this.failMessage = failMessage;
	}

	/**
	 * 환불 성공
	 */
	public void markCancelled() {
		if (this.status == PaymentStatus.CANCELLED) return; // 멱등
		if (!isRefundable()) {
			throw new IllegalStateException("Payment is not refundable. status=" + status);
		}
		this.status = PaymentStatus.CANCELLED;
		this.refundFailCode = null;
		this.refundFailMessage = null;
	}

	/**
	 * 환불 실패: 상태는 PAID 유지 + 실패 사유만 기록
	 */
	public void markRefundFailed(String failCode, String failMessage) {
		if (this.status == PaymentStatus.CANCELLED) {
			throw new IllegalStateException("Already cancelled payment.");
		}
		if (this.status != PaymentStatus.PAID) {
			// READY/FAILED 에서 환불 실패 기록은 의미가 약해서 막아도 됨
			throw new IllegalStateException("Refund fail is only meaningful when PAID. status=" + status);
		}
		this.refundFailCode = failCode;
		this.refundFailMessage = failMessage;
	}
}
