package com.groom.e_commerce.order.domain.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.groom.e_commerce.global.domain.entity.BaseEntity;
import com.groom.e_commerce.order.domain.status.OrderStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_order")
public class Order extends BaseEntity {

	/* ================= 식별자 ================= */

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_id", nullable = false, unique = true)
	private UUID orderId;

	@Column(name = "order_number", nullable = false, unique = true, length = 20)
	private String orderNumber;

	@Column(name = "buyer_id", nullable = false)
	private UUID buyerId;

	/* ================= 금액 / 상태 ================= */

	@Column(name = "total_payment_amt", nullable = false)
	private Long totalPaymentAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrderStatus status;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<OrderItem> items = new ArrayList<>();

	/* ================= 배송지 스냅샷 ================= */

	@Column(name = "recipient_name", nullable = false, length = 50)
	private String recipientName;

	@Column(name = "recipient_phone", nullable = false, length = 20)
	private String recipientPhone;

	@Column(name = "zip_code", nullable = false, length = 10)
	private String zipCode;

	@Column(name = "shipping_address", nullable = false, length = 300)
	private String shippingAddress;

	@Column(name = "shipping_memo", length = 200)
	private String shippingMemo;

	/* ================= 생성 ================= */

	@Builder
	public Order(
		UUID buyerId,
		String orderNumber,
		Long totalPaymentAmount,
		String recipientName,
		String recipientPhone,
		String zipCode,
		String shippingAddress,
		String shippingMemo
	) {
		this.orderId = UUID.randomUUID();
		this.buyerId = buyerId;
		this.orderNumber = orderNumber;
		this.totalPaymentAmount = totalPaymentAmount;
		this.recipientName = recipientName;
		this.recipientPhone = recipientPhone;
		this.zipCode = zipCode;
		this.shippingAddress = shippingAddress;
		this.shippingMemo = shippingMemo;
		this.status = OrderStatus.PENDING;
	}

	/* ================= 상태 전이 ================= */
	/* 전부 "이벤트 수신 결과"로만 호출된다고 가정 */

	/** 결제 성공 이벤트 수신 */
	public void markPaid() {
		validateStatus(OrderStatus.PENDING, "결제 완료 처리는 PENDING 상태에서만 가능합니다.");
		this.status = OrderStatus.PAID;
	}

	/** 결제 실패 이벤트 수신 */
	public void markPaymentFailed() {
		validateStatus(OrderStatus.PENDING, "결제 실패 처리는 PENDING 상태에서만 가능합니다.");
		this.status = OrderStatus.FAILED;
	}

	/** 재고 차감 성공 이벤트 수신 */
	public void confirmStock() {
		validateStatus(OrderStatus.PAID, "재고 확정은 PAID 상태에서만 가능합니다.");
		this.status = OrderStatus.CONFIRMED;
	}

	/** 환불 성공 이벤트 수신 */
	public void markCancelled() {
		if (this.status != OrderStatus.PAID && this.status != OrderStatus.CONFIRMED) {
			throw new IllegalStateException("환불 처리는 PAID 또는 CONFIRMED 상태에서만 가능합니다.");
		}
		this.status = OrderStatus.CANCELLED;
	}

	/** 환불 실패 이벤트 수신 */
	public void markManualCheck() {
		this.status = OrderStatus.MANUAL_CHECK;
	}

	/* ================= 조회용 헬퍼 ================= */

	public boolean isPayable() {
		return this.status == OrderStatus.PENDING;
	}

	public boolean isStockConfirmable() {
		return this.status == OrderStatus.PAID;
	}

	public boolean isRefundable() {
		return this.status == OrderStatus.PAID || this.status == OrderStatus.CONFIRMED;
	}

	/* ================= 내부 유틸 ================= */

	private void validateStatus(OrderStatus expected, String message) {
		if (this.status != expected) {
			throw new IllegalStateException(
				String.format("%s (현재 상태: %s)", message, this.status)
			);
		}
	}
}
