package com.groom.e_commerce.global.infrastructure.client.Classification;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.groom.e_commerce.global.infrastructure.client.Classification.dto.AiFeignRequest;
import com.groom.e_commerce.global.infrastructure.client.Classification.dto.AiFeignResponse;

class AiFeignFallbackTest {

	@Test
	void fallback_returnsErr() {
		AiFeignFallback fallback = new AiFeignFallback();

		AiFeignResponse res =
			fallback.classify(new AiFeignRequest("test"));

		assertThat(res.category()).isEqualTo("ERR");
		assertThat(res.confidence()).isZero();
	}
}
