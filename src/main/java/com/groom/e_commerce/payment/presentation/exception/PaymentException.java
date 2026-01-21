package com.groom.e_commerce.payment.presentation.exception;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;

/**
 * 결제 도메인 내부 비즈니스 예외
 * (결제 상태 불일치, 중복 요청, 취소 불가 등)
 */
public class PaymentException extends CustomException {

	public PaymentException(ErrorCode errorCode) {
		super(errorCode);
	}

	public PaymentException(ErrorCode errorCode, String debugMessage) {
		super(errorCode, debugMessage);
	}
}
