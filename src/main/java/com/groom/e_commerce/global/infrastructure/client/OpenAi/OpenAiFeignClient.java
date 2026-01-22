package com.groom.e_commerce.global.infrastructure.client.OpenAi;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import com.groom.e_commerce.global.infrastructure.client.OpenAi.config.OpenAiFeignConfig;
import com.groom.e_commerce.global.infrastructure.client.OpenAi.dto.ChatCompletionRequest;
import com.groom.e_commerce.global.infrastructure.client.OpenAi.dto.ChatCompletionResponse;

@FeignClient(
	name = "openai-client",
	url = "${ai.openai.base-url}",
	configuration = OpenAiFeignConfig.class,
	fallback = OpenAiFeignFallback.class
)
public interface OpenAiFeignClient {

	@PostMapping("/v1/chat/completions")
	ChatCompletionResponse createChatCompletion(
		ChatCompletionRequest request
	);
}
