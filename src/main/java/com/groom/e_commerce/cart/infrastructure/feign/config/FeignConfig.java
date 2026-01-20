package com.groom.e_commerce.cart.infrastructure.feign.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Logger;
import feign.Request;
import feign.codec.ErrorDecoder;

@Configuration
public class FeignConfig {

	/**
	 * Feign Timeout
	 * - Order 검증은 fail-fast
	 */
	@Bean
	public Request.Options feignRequestOptions() {
		return new Request.Options(
			300, TimeUnit.MILLISECONDS,   // connect timeout
			500, TimeUnit.MILLISECONDS,   // read timeout
			true
		);
	}

	/**
	 * Feign Logging
	 * - Review <-> Order 호출 추적용
	 */
	@Bean
	public Logger.Level feignLoggerLevel() {
		return Logger.Level.BASIC;
	}

	/**
	 * Feign Error Decoder
	 */
	@Bean
	public ErrorDecoder feignErrorDecoder() {
		return new FeignErrorDecoder();
	}
}
