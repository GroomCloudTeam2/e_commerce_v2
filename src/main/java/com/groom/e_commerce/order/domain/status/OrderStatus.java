package com.groom.e_commerce.order.domain.status;

public enum OrderStatus {
    PENDING,        // 주문 생성됨 (결제 대기)
    PAID,           // 결제 완료
    FAILED,         // 주문 실패 (결제 실패, 재고 부족 등)
    CONFIRMED,      // 주문 확정 (결제 완료, 재고 확보 완료)
    SHIPPING,       // 배송중
    DELIVERED,      // 배송 완료
    COMPLETED,      // 구매 확정
    CANCELLED,      // 주문 취소 (환불 완료)
    MANUAL_CHECK    // 수동 확인 필요 (환불 실패 등)
}