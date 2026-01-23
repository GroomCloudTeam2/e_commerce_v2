package org.example.payment.infrastructure.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.payment.infrastructure.config.TossPaymentsProperties;
import com.groom.e_commerce.payment.presentation.exception.TossApiException;

import feign.Request;
import feign.RequestInterceptor;
import feign.Response;
import feign.codec.ErrorDecoder;

@Configuration
public class TossFeignConfig {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Bean
	public Request.Options tossfeignRequestOptions(TossPaymentsProperties props) {
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
		if (tossCode != null && !tossCode.isBlank()) {
			if ("UNAUTHORIZED_KEY".equals(tossCode) || "INVALID_API_KEY".equals(tossCode)) {
				return ErrorCode.TOSS_UNAUTHORIZED_KEY;
			}
			if ("PROVIDER_ERROR".equals(tossCode)) {
				return ErrorCode.TOSS_PROVIDER_ERROR;
			}
			if ("NOT_FOUND_PAYMENT".equals(tossCode) || "NOT_FOUND_PAYMENT_SESSION".equals(tossCode)) {
				return ErrorCode.PAYMENT_NOT_FOUND;
			}
			return ErrorCode.TOSS_REJECTED;
		}

		if (httpStatus == 401) return ErrorCode.TOSS_UNAUTHORIZED_KEY;
		if (httpStatus == 404) return ErrorCode.PAYMENT_NOT_FOUND;
		if (httpStatus >= 500) return ErrorCode.TOSS_PROVIDER_ERROR;
		return ErrorCode.TOSS_REJECTED;
	}

	private record TossErrorBody(String code, String message) {
	}
}
