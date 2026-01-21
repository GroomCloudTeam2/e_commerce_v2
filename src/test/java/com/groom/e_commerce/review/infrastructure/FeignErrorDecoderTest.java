package com.groom.e_commerce.review.infrastructure;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.review.infrastructure.feign.config.FeignErrorDecoder;

import feign.Request;
import feign.Response;

class FeignErrorDecoderTest {

	private final FeignErrorDecoder decoder = new FeignErrorDecoder();

	@Test
	@DisplayName("404 응답 → ORDER_NOT_FOUND")
	void decode_404() {
		Exception ex = decoder.decode("OrderClient#validate", response(404));

		assertThat(ex)
			.isInstanceOf(CustomException.class)
			.extracting(e -> ((CustomException) e).getErrorCode())
			.isEqualTo(ErrorCode.ORDER_NOT_FOUND);
	}

	@Test
	@DisplayName("403 응답 → FORBIDDEN")
	void decode_403() {
		Exception ex = decoder.decode("OrderClient#validate", response(403));

		assertThat(ex)
			.isInstanceOf(CustomException.class)
			.extracting(e -> ((CustomException) e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("409 응답 → REVIEW_NOT_ALLOWED_ORDER_STATUS")
	void decode_409() {
		Exception ex = decoder.decode("OrderClient#validate", response(409));

		assertThat(ex)
			.isInstanceOf(CustomException.class)
			.extracting(e -> ((CustomException) e).getErrorCode())
			.isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED_ORDER_STATUS);
	}

	@Test
	@DisplayName("500 응답 → ORDER_SERVICE_ERROR")
	void decode_500() {
		Exception ex = decoder.decode("OrderClient#validate", response(500));

		assertThat(ex)
			.isInstanceOf(CustomException.class)
			.extracting(e -> ((CustomException) e).getErrorCode())
			.isEqualTo(ErrorCode.ORDER_SERVICE_ERROR);
	}

	private Response response(int status) {
		return Response.builder()
			.status(status)
			.request(Request.create(
				Request.HttpMethod.POST,
				"/internal/api/v1/review/isReviewable",
				Collections.emptyMap(), // ⭐ headers는 필수
				null,
				StandardCharsets.UTF_8
			))
			.build();
	}
}
