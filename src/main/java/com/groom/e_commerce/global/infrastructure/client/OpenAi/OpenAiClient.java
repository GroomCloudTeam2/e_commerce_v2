package com.groom.e_commerce.global.infrastructure.client.OpenAi;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.groom.e_commerce.global.infrastructure.client.OpenAi.dto.ChatCompletionRequest;
import com.groom.e_commerce.global.infrastructure.client.OpenAi.dto.ChatCompletionResponse;
import com.groom.e_commerce.global.infrastructure.client.OpenAi.dto.Message;

@Component
public class OpenAiClient {

	private final OpenAiFeignClient feignClient;
	private final String model;

	public OpenAiClient(
		OpenAiFeignClient feignClient,
		@Value("${ai.openai.model}") String model
	) {
		this.feignClient = feignClient;
		this.model = model;
	}

	public String summarizeReviews(String prompt) {
		ChatCompletionResponse response =
			feignClient.createChatCompletion(
				new ChatCompletionRequest(
					model,
					List.of(
						new Message("system", "너는 이커머스 상품 리뷰 요약 전문가야."),
						new Message("user", prompt)
					),
					0.3
				)
			);

		return response.getContent();
	}
}
