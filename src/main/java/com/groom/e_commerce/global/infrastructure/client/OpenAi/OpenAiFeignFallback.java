package com.groom.e_commerce.global.infrastructure.client.OpenAi;

import java.util.List;

import org.springframework.stereotype.Component;
import com.groom.e_commerce.global.infrastructure.client.OpenAi.dto.Message;

import com.groom.e_commerce.global.infrastructure.client.OpenAi.dto.ChatCompletionRequest;
import com.groom.e_commerce.global.infrastructure.client.OpenAi.dto.ChatCompletionResponse;
import com.groom.e_commerce.global.infrastructure.client.OpenAi.dto.Choice;

@Component
public class OpenAiFeignFallback implements OpenAiFeignClient {

	@Override
	public ChatCompletionResponse createChatCompletion(
		ChatCompletionRequest request
	) {
		// 정책: 요약 실패 시 null or 빈 응답
		return new ChatCompletionResponse(
			List.of(
				new Choice(
					new Message("assistant", "")
				)
			)
		);
	}
}
