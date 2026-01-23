package org.example.payment.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Request;

@Configuration
public class InternalFeignConfig {

	@Bean
	public Request.Options internalFeignRequestOptions() {
		return new Request.Options(1500, 2500);
	}
}
