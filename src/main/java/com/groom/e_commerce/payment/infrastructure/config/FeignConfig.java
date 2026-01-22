package com.groom.e_commerce.payment.infrastructure.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.payment.presentation.exception.TossApiException;

import feign.Request;
import feign.RequestInterceptor;
import feign.Response;
import feign.codec.ErrorDecoder;

@Configuration
public class FeignConfig {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Bean
	public Request.Options feignRequestOptions(TossPaymentsProperties props) {
		int connectTimeout = props.connectTimeoutMs() != null ? props.connectTimeoutMs() : 3000;
		int readTimeout = props.readTimeoutMs() != null ? props.readTimeoutMs() : 5000;
		return new Request.Options(connectTimeout, readTimeout);
	}

	/**
	 * Toss API 전용 Auth 헤더
	 */
	@Bean
	public RequestInterceptor tossAuthInterceptor(TossPaymentsProperties props) {
		return template -> {
			String secretKey = props.secretKey();
			if (secretKey == null || secretKey.isBlank()) return;

			String token = Base64.getEncoder()
				.encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

			template.header("Authorization", "Basic " + token);
			template.header("Content-Type", "application/json");
			template.header("Accept", "application/json");
		};
	}

	/**
	 * Toss API 에러 응답을 TossApiException(CustomException)으로 변환
	 */
	@Bean
	public ErrorDecoder tossErrorDecoder() {
		return (methodKey, response) -> {
			int status = response.status();

			TossErrorBody body = readTossErrorBody(response);
			ErrorCode mapped = mapToErrorCode(body.code(), status);

			// debugMessage는 로그/추적용 (클라이언트 응답 메시지는 ErrorCode.message 사용)
			String debug = "Toss API call failed. method=" + methodKey
				+ ", status=" + status
				+ ", tossCode=" + body.code()
				+ ", tossMessage=" + body.message();

			return new TossApiException(mapped, debug);
		};
	}

	private TossErrorBody readTossErrorBody(Response response) {
		if (response == null || response.body() == null) {
			return new TossErrorBody(null, null);
		}
		try {
			String raw = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
			if (raw.isBlank()) return new TossErrorBody(null, null);

			JsonNode root = OBJECT_MAPPER.readTree(raw);
			String code = root.has("code") ? root.get("code").asText(null) : null;
			String message = root.has("message") ? root.get("message").asText(null) : null;

			return new TossErrorBody(code, message);
		} catch (IOException e) {
			return new TossErrorBody(null, null);
		}
	}

	private ErrorCode mapToErrorCode(String tossCode, int httpStatus) {
		// 1) Toss code 우선 매핑
		if (tossCode != null && !tossCode.isBlank()) {

			// 인증/키 계열
			if ("UNAUTHORIZED_KEY".equals(tossCode) || "INVALID_API_KEY".equals(tossCode)) {
				return ErrorCode.TOSS_UNAUTHORIZED_KEY;
			}

			// 일시 장애(재시도 유도)
			if ("PROVIDER_ERROR".equals(tossCode)) {
				return ErrorCode.TOSS_PROVIDER_ERROR;
			}

			// "없음" 계열은 내부 도메인 에러로 맞추는 게 더 자연스러움
			if ("NOT_FOUND_PAYMENT".equals(tossCode) || "NOT_FOUND_PAYMENT_SESSION".equals(tossCode)) {
				return ErrorCode.PAYMENT_NOT_FOUND;
			}

			// 나머지(거절/정책/한도/카드/은행시간/FDS 등) → 통합
			return ErrorCode.TOSS_REJECTED;
		}

		// 2) Toss code 없으면 status로 fallback
		if (httpStatus == 401) return ErrorCode.TOSS_UNAUTHORIZED_KEY;
		if (httpStatus == 404) return ErrorCode.PAYMENT_NOT_FOUND;

		// 5xx는 보통 Toss 장애/일시 오류로 묶기
		if (httpStatus >= 500) return ErrorCode.TOSS_PROVIDER_ERROR;

		// 그 외는 거절로 통일
		return ErrorCode.TOSS_REJECTED;
	}

	private record TossErrorBody(String code, String message) {}
}
