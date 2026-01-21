package com.groom.e_commerce.product.presentation.dto.response;

import java.util.List;
import java.util.UUID;

import com.groom.e_commerce.product.application.dto.ProductCartInfo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Internal API - 상품 정보 벌크 조회 응답 DTO
 * Order/Cart 서비스에서 상품 정보를 조회할 때 사용
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ResProductBulkInfoDto {

	private List<ProductInfo> products;

	@Getter
	@Builder
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	public static class ProductInfo {
		private UUID productId;
		private UUID ownerId;
		private UUID variantId;
		private String productName;
		private String optionName;
		private String thumbnailUrl;
		private Long price;
		private Integer stockQuantity;
		private boolean available;

		public static ProductInfo from(ProductCartInfo info) {
			return ProductInfo.builder()
				.productId(info.getProductId())
				.ownerId(info.getOwnerId())
				.variantId(info.getVariantId())
				.productName(info.getProductName())
				.optionName(info.getOptionName())
				.thumbnailUrl(info.getThumbnailUrl())
				.price(info.getPrice())
				.stockQuantity(info.getStockQuantity())
				.available(info.isAvailable())
				.build();
		}
	}

	public static ResProductBulkInfoDto from(List<ProductCartInfo> infos) {
		List<ProductInfo> products = infos.stream()
			.map(ProductInfo::from)
			.toList();

		return ResProductBulkInfoDto.builder()
			.products(products)
			.build();
	}
}
