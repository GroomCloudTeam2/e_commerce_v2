package com.groom.e_commerce.order.domain.status;

public enum OrderStatus {
	PENDING,            // 결제 대기 (OrderCreatedEvent 발행 후)
	PAID,               // 결제 완료 (PaymentCompletedEvent 수신 시)
	CONFIRMED,          // 재고 확정 완료 (StockDeductedEvent 수신 시)

	// --- 실패/취소 관련 ---
	FAILED,             // 결제 실패 (PaymentFailEvent 수신 시, 돈 안 나감)
	CANCELLED,          // 주문 취소/환불 완료 (RefundSucceededEvent 수신 시, 돈 나갔다 들어옴)
	MANUAL_CHECK        // 운영자 개입 (RefundFailedEvent 수신 시)
}
	//
	// public boolean canShip() {
	// 	return this == PAID;
	// }

	// public boolean canDeliver() {
	// 	return this == SHIPPING;
	// }

	// public boolean canConfirm() {
	// 	return this == DELIVERED;
	// }
}
