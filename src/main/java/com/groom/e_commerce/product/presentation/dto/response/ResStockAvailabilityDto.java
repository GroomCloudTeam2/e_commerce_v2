package com.groom.e_commerce.product.presentation.dto.response;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Internal API - 가용 재고 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ResStockAvailabilityDto {

	private UUID productId;
	private UUID variantId;
	private Integer availableStock;
	private boolean exists;

	public static ResStockAvailabilityDto of(UUID productId, UUID variantId, Integer stock) {
		return ResStockAvailabilityDto.builder()
			.productId(productId)
			.variantId(variantId)
			.availableStock(stock)
			.exists(stock != null)
			.build();
	}
}
