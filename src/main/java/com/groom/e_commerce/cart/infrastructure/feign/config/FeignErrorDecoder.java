package com.groom.e_commerce.cart.infrastructure.feign.config;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;

import feign.Response;
import feign.codec.ErrorDecoder;

public class FeignErrorDecoder implements ErrorDecoder {

	private final ErrorDecoder defaultDecoder = new Default();

	@Override
	public Exception decode(String methodKey, Response response) {

		return switch (response.status()) {
			case 400 -> new CustomException(ErrorCode.INVALID_REQUEST);
			case 403 -> new CustomException(ErrorCode.FORBIDDEN);
			case 404 -> new CustomException(ErrorCode.ORDER_NOT_FOUND);
			case 500, 502, 503 ->
				new CustomException(ErrorCode.ORDER_SERVICE_ERROR);
			default -> defaultDecoder.decode(methodKey, response);
		};
	}
}
