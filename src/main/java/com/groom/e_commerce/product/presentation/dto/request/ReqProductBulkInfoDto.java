package com.groom.e_commerce.product.presentation.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Internal API - 상품 정보 벌크 조회 요청 DTO
 * Order/Cart 서비스에서 OpenFeign을 통해 호출
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReqProductBulkInfoDto {

	@NotEmpty(message = "조회할 상품 항목은 필수입니다.")
	@Valid
	private List<ProductItem> items;

	@Getter
	@Builder
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	public static class ProductItem {

		@NotNull(message = "상품 ID는 필수입니다.")
		private UUID productId;

		private UUID variantId;

		private int quantity;
	}
}
