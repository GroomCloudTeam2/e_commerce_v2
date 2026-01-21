package com.groom.e_commerce.cart.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.groom.e_commerce.cart.application.dto.StockManagement;
import com.groom.e_commerce.cart.infrastructure.feign.ProductClientFallback;
import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;

class ProductClientFallbackTest {

	private final ProductClientFallback fallback = new ProductClientFallback();

	@Test
	void getProductCartInfos_shouldThrowProductServiceError() {
		// given
		List<StockManagement> requests = List.of(
			StockManagement.of(
				UUID.randomUUID(),
				UUID.randomUUID(),
				1
			)
		);

		// when & then
		assertThatThrownBy(() ->
			fallback.getProductCartInfos(requests)
		)
		.isInstanceOf(CustomException.class)
		.satisfies(ex -> {
			CustomException ce = (CustomException) ex;
			assertThat(ce.getErrorCode())
				.isEqualTo(ErrorCode.PRODUCT_SERVICE_ERROR);
		});
	}
}
