package com.groom.e_commerce.payment.presentation.exception;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;

/**
 * Toss Payments 외부 연동 실패 예외
 * (Feign / API 응답 에러)
 */
public class TossApiException extends CustomException {

	public TossApiException(ErrorCode errorCode) {
		super(errorCode);
	}

	public TossApiException(ErrorCode errorCode, String debugMessage) {
		super(errorCode, debugMessage);
	}
}
