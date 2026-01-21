package com.groom.e_commerce.product.presentation.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Internal API - 재고 관련 작업 요청 DTO
 * Order/Cart 서비스에서 OpenFeign을 통해 호출
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReqStockOperationDto {

	@NotEmpty(message = "재고 작업 항목은 필수입니다.")
	@Valid
	private List<StockItem> items;

	@Getter
	@Builder
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	public static class StockItem {

		@NotNull(message = "상품 ID는 필수입니다.")
		private UUID productId;

		private UUID variantId;

		@Min(value = 1, message = "수량은 1 이상이어야 합니다.")
		private int quantity;
	}
}
