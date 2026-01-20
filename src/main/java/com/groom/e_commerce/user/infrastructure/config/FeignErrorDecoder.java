package com.groom.e_commerce.user.infrastructure.config;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

	@Override
	public Exception decode(String methodKey, Response response) {
		log.error("Feign 호출 에러 - method: {}, status: {}, reason: {}",
			methodKey, response.status(), response.reason());

		return switch (response.status()) {
			case 400 -> new CustomException(ErrorCode.BAD_REQUEST, "잘못된 요청입니다.");
			case 401 -> new CustomException(ErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
			case 403 -> new CustomException(ErrorCode.FORBIDDEN, "접근 권한이 없습니다.");
			case 404 -> new CustomException(ErrorCode.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다.");
			case 503 -> new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "서비스를 일시적으로 사용할 수 없습니다.");
			default -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "외부 서비스 호출 중 오류가 발생했습니다.");
		};
	}
}
