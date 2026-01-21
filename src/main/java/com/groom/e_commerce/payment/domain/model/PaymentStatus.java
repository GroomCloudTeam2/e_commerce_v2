package com.groom.e_commerce.payment.domain.model;

public enum PaymentStatus {
	READY,      // 주문 생성 이벤트 수신 후 결제 대기
	PAID,       // 토스 승인(DONE) 확정
	FAILED,     // 토스 승인 실패(ABORTED/EXPIRED 등)
	CANCELLED   // 환불 성공
}
