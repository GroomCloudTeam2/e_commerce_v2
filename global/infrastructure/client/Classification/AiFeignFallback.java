package com.groom.e_commerce.global.infrastructure.client.Classification;

import org.springframework.stereotype.Component;

import com.groom.e_commerce.global.infrastructure.client.Classification.dto.AiFeignRequest;
import com.groom.e_commerce.global.infrastructure.client.Classification.dto.AiFeignResponse;

@Component
public class AiFeignFallback implements AiFeignClient {

	@Override
	public AiFeignResponse classify(AiFeignRequest request) {
		return new AiFeignResponse("ERR", 0.0);
	}
}
