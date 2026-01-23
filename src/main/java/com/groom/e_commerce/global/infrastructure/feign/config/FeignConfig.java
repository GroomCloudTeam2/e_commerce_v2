package com.groom.e_commerce.global.infrastructure.feign.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.groom.e_commerce.payment.infrastructure.config.TossPaymentsProperties;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FeignConfig {

	private final TossPaymentsProperties tossProps;

	@Bean
	public Request.Options feignRequestOptions() {
		return new Request.Options(
			5000, TimeUnit.MILLISECONDS,
			5000, TimeUnit.MILLISECONDS,
			true
		);
	}

	@Bean
	public Logger.Level feignLoggerLevel() {
		return Logger.Level.FULL;
	}

	@Bean
	public RequestInterceptor requestInterceptor() {
		return template -> {
			String targetUrl = template.feignTarget().url();

			// ⚔️ [핵심 해결책]
			// 무엇이 들어있든 간에, 일단 Authorization 헤더를 비웁니다.
			// 이걸 안 하면 'Bearer dummy'와 'Basic ...'이 같이 날아갑니다.
			template.removeHeader("Authorization");

			// 1. Toss API 요청인 경우
			if (targetUrl.contains("tosspayments.com")) {
				String secretKey = tossProps.secretKey().trim();
				String token = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

				template.header("Authorization", "Basic " + token);
				template.header("Content-Type", "application/json"); // Content-Type도 확실하게 지정

				log.info(">>> [Feign-Global] Toss API Detected. Headers Cleaned & Basic Auth Applied.");
			}
			// 2. 그 외 일반 요청 (내부 MSA 등)
			else {
				template.header("Authorization", "Bearer dummy");
				log.info(">>> [Feign-Global] Internal API. Bearer Token Applied.");
			}
		};
	}

	@Bean
	public ErrorDecoder feignErrorDecoder() {
		return new FeignErrorDecoder();
	}
}
