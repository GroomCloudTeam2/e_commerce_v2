package com.groom.e_commerce.order.domain.entity;

import com.groom.e_commerce.global.domain.entity.BaseEntity;
import com.groom.e_commerce.order.domain.status.OrderStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    /**
     * 결제 성공 처리. PENDING 상태일 때만 PAID로 변경.
     * @return 상태 변경이 일어났으면 true, 아니면 false.
     */
    public boolean markPaid() {
        if (this.status == OrderStatus.PENDING) {
            this.status = OrderStatus.PAID;
            return true;
        }
        return false;
    }

    /**
     * 결제 실패 처리. PENDING 상태일 때만 FAILED로 변경.
     * @return 상태 변경이 일어났으면 true, 아니면 false.
     */
    public boolean markFailed() {
        if (this.status == OrderStatus.PENDING) {
            this.status = OrderStatus.FAILED;
            return true;
        }
        return false;
    }

    /**
     * 주문 확정 처리 (재고 확보 완료). PAID 상태일 때만 CONFIRMED로 변경.
     * @return 상태 변경이 일어났으면 true, 아니면 false.
     */
    public boolean markConfirmed() {
        if (this.status == OrderStatus.PAID) {
            this.status = OrderStatus.CONFIRMED;
            return true;
        }
        return false;
    }

    /**
     * 주문 취소 처리.
     * @return 상태 변경이 일어났으면 true, 아니면 false.
     */
	public boolean markCancelled() {
		if (this.status == OrderStatus.SHIPPING ||
			this.status == OrderStatus.DELIVERED ||
			this.status == OrderStatus.COMPLETED) {
			throw new IllegalStateException("이미 배송이 시작되었거나 완료된 주문은 취소할 수 없습니다.");
		}

		if (this.status != OrderStatus.CANCELLED) {
			this.status = OrderStatus.CANCELLED;
			return true;
		}
		return false;
	}

    /**
     * 수동 확인 필요 처리.
     * @return 상태 변경이 일어났으면 true, 아니면 false.
     */
	public boolean needsManualCheck() {
		if (this.status == OrderStatus.COMPLETED || this.status == OrderStatus.CANCELLED) {
			throw new IllegalStateException("최종 완료되거나 취소된 주문은 수동 확인 상태로 변경할 수 없습니다.");
		}

		if (this.status != OrderStatus.MANUAL_CHECK) {
			this.status = OrderStatus.MANUAL_CHECK;
			return true;
		}
		return false;
	}
}
