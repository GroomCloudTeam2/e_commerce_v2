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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_order")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

	@Column(name = "order_number", nullable = false, unique = true)
	private String orderNumber;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

	@Column(name = "total_payment_amount", nullable = false)
	private BigInteger totalPaymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> item = new ArrayList<>();


	public Order(UUID buyerId, String orderNumber, BigInteger totalAmount, List<OrderItem> items) {
		this.orderId = UUID.randomUUID();
		this.orderNumber = orderNumber;
		this.buyerId = buyerId;
		this.totalPaymentAmount = totalAmount;
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
