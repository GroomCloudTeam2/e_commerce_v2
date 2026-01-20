package com.groom.e_commerce.user.infrastructure.config;

import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;

@Configuration
public class FeignConfig {

	/**
	 * 요청 인터셉터 - 헤더 추가
	 */
	@Bean
	public RequestInterceptor requestInterceptor() {
		return requestTemplate -> {
			// 내부 서비스 호출 표시
			requestTemplate.header("X-Internal-Call", "true");

			// 분산 추적용 traceId 전파
			String traceId = MDC.get("traceId");
			if (traceId != null) {
				requestTemplate.header("X-Trace-Id", traceId);
			}

			// 요청 ID
			String requestId = MDC.get("requestId");
			if (requestId != null) {
				requestTemplate.header("X-Request-Id", requestId);
			}
		};
	}

	/**
	 * 에러 디코더
	 */
	@Bean
	public ErrorDecoder errorDecoder() {
		return new FeignErrorDecoder();
	}

	/**
	 * 재시도 설정
	 * - 100ms 간격
	 * - 최대 1초
	 * - 3회 재시도
	 */
	@Bean
	public Retryer retryer() {
		return new Retryer.Default(100, 1000, 3);
	}

	/**
	 * 로그 레벨
	 * - NONE: 로깅 안함
	 * - BASIC: 요청 메서드, URL, 응답 상태 코드, 실행 시간
	 * - HEADERS: BASIC + 요청/응답 헤더
	 * - FULL: 헤더, 바디, 메타데이터 전체
	 */
	@Bean
	public Logger.Level feignLoggerLevel() {
		return Logger.Level.BASIC;
	}

	/**
	 * 타임아웃 설정
	 */
	@Bean
	public Request.Options requestOptions() {
		return new Request.Options(
			5, TimeUnit.SECONDS,   // connectTimeout
			10, TimeUnit.SECONDS,  // readTimeout
			true                    // followRedirects
		);
	}
}
