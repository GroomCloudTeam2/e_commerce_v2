package com.groom.e_commerce.global.infrastructure.client.OpenAi.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionResponse {
	private List<Choice> choices;

	public String getContent() {
		return choices.get(0).getMessage().getContent();
	}
}
