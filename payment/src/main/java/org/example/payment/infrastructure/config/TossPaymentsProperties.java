package org.example.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss.payments")
public record TossPaymentsProperties(
	String baseUrl,
	String secretKey,
	String clientKey,     // 프론트에 내려줄 용도(ready에서 쓰면)
	String successUrl,    // ready에서 쓰면
	String failUrl,       // ready에서 쓰면
	Integer connectTimeoutMs,
	Integer readTimeoutMs
) {
}
