package com.groom.e_commerce.payment.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "p_payment_cancel",
	indexes = {
		@Index(name = "ix_payment_cancel_payment_id", columnList = "payment_id"),
		@Index(name = "ix_payment_cancel_payment_key", columnList = "payment_key")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentCancel {

	@Id
	@Column(name = "cancel_id", columnDefinition = "uuid")
	private UUID cancelId;

	@Column(name = "payment_id", nullable = false, columnDefinition = "uuid")
	private UUID paymentId;

	@Column(name = "cancel_amount", nullable = false)
	private Long cancelAmount;

	@Column(name = "canceled_at", nullable = false)
	private LocalDateTime canceledAt;

	@Column(name = "payment_key", length = 200)
	private String paymentKey;

	@PrePersist
	void onCreate() {
		if (this.cancelId == null) this.cancelId = UUID.randomUUID();
		if (this.canceledAt == null) this.canceledAt = LocalDateTime.now();
	}

	private PaymentCancel(UUID paymentId, String paymentKey, Long cancelAmount, LocalDateTime canceledAt) {
		this.cancelId = UUID.randomUUID();
		this.paymentId = paymentId;
		this.paymentKey = paymentKey;
		this.cancelAmount = cancelAmount;
		this.canceledAt = canceledAt != null ? canceledAt : LocalDateTime.now();
	}

	public static PaymentCancel of(UUID paymentId, String paymentKey, Long cancelAmount, LocalDateTime canceledAt) {
		if (paymentId == null) throw new IllegalArgumentException("paymentId is null");
		if (cancelAmount == null || cancelAmount <= 0) throw new IllegalArgumentException("cancelAmount is invalid");
		return new PaymentCancel(paymentId, paymentKey, cancelAmount, canceledAt);
	}
}
