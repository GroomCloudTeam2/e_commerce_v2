package com.groom.e_commerce.product.infrastructure.cache;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.entity.ProductOption;
import com.groom.e_commerce.product.domain.entity.ProductOptionValue;
import com.groom.e_commerce.product.domain.entity.ProductVariant;
import com.groom.e_commerce.product.domain.enums.ProductStatus;
import com.groom.e_commerce.product.domain.enums.VariantStatus;
import com.groom.e_commerce.product.presentation.dto.response.ResProductDetailDtoV1;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 상세 Redis 캐시용 DTO
 * ResProductDetailDtoV1과 동일한 구조, Redis 직렬화에 최적화
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CachedProductDetail implements Serializable {

	private static final long serialVersionUID = 1L;

	private UUID productId;
	private UUID ownerId;
	private UUID categoryId;
	private String categoryName;
	private String categoryFullPath;

	private String title;
	private String description;
	private String thumbnailUrl;
	private Long price;
	private Long minPrice;
	private Long maxPrice;
	private Integer stockQuantity;
	private ProductStatus status;
	private Boolean hasOptions;

	private List<CachedOption> options;
	private List<CachedVariant> variants;

	private LocalDateTime createdAt;
	private long cachedAt;  // 캐시 저장 시간 (epoch millis)

	@Getter
	@Builder
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	public static class CachedOption implements Serializable {
		private static final long serialVersionUID = 1L;

		private UUID optionId;
		private String name;
		private Integer sortOrder;
		private List<CachedOptionValue> values;

		public static CachedOption from(ProductOption option) {
			return CachedOption.builder()
				.optionId(option.getId())
				.name(option.getName())
				.sortOrder(option.getSortOrder())
				.values(option.getOptionValues().stream()
					.map(CachedOptionValue::from)
					.collect(Collectors.toList()))
				.build();
		}
	}

	@Getter
	@Builder
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	public static class CachedOptionValue implements Serializable {
		private static final long serialVersionUID = 1L;

		private UUID optionValueId;
		private String value;

		public static CachedOptionValue from(ProductOptionValue optionValue) {
			return CachedOptionValue.builder()
				.optionValueId(optionValue.getId())
				.value(optionValue.getValue())
				.build();
		}
	}

	@Getter
	@Builder
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	public static class CachedVariant implements Serializable {
		private static final long serialVersionUID = 1L;

		private UUID variantId;
		private String skuCode;
		private List<UUID> optionValueIds;
		private String optionName;
		private Long price;
		private Integer stockQuantity;
		private VariantStatus status;

		public static CachedVariant from(ProductVariant variant) {
			return CachedVariant.builder()
				.variantId(variant.getId())
				.skuCode(variant.getSkuCode())
				.optionValueIds(variant.getOptionValueIds())
				.optionName(variant.getOptionName())
				.price(variant.getPrice())
				.stockQuantity(variant.getStockQuantity())
				.status(variant.getStatus())
				.build();
		}
	}

	/**
	 * Product 엔티티로부터 캐시 객체 생성
	 */
	public static CachedProductDetail from(Product product) {
		Long minPrice = product.getPrice();
		Long maxPrice = product.getPrice();

		if (Boolean.TRUE.equals(product.getHasOptions())
			&& product.getVariants() != null
			&& !product.getVariants().isEmpty()) {
			minPrice = product.getVariants().stream()
				.map(ProductVariant::getPrice)
				.min(Long::compareTo)
				.orElse(product.getPrice());
			maxPrice = product.getVariants().stream()
				.map(ProductVariant::getPrice)
				.max(Long::compareTo)
				.orElse(product.getPrice());
		}

		return CachedProductDetail.builder()
			.productId(product.getId())
			.ownerId(product.getOwnerId())
			.categoryId(product.getCategory().getId())
			.categoryName(product.getCategory().getName())
			.categoryFullPath(buildCategoryPath(product))
			.title(product.getTitle())
			.description(product.getDescription())
			.thumbnailUrl(product.getThumbnailUrl())
			.price(product.getPrice())
			.minPrice(minPrice)
			.maxPrice(maxPrice)
			.stockQuantity(product.getStockQuantity())
			.status(product.getStatus())
			.hasOptions(product.getHasOptions())
			.options(product.getOptions().stream()
				.map(CachedOption::from)
				.collect(Collectors.toList()))
			.variants(product.getVariants().stream()
				.map(CachedVariant::from)
				.collect(Collectors.toList()))
			.createdAt(product.getCreatedAt())
			.cachedAt(System.currentTimeMillis())
			.build();
	}

	/**
	 * 캐시 객체를 응답 DTO로 변환
	 */
	public ResProductDetailDtoV1 toResponseDto() {
		return ResProductDetailDtoV1.builder()
			.productId(this.productId)
			.title(this.title)
			.description(this.description)
			.thumbnailUrl(this.thumbnailUrl)
			.price(this.price)
			.status(this.status)
			.hasOptions(this.hasOptions)
			.category(ResProductDetailDtoV1.CategoryInfo.builder()
				.categoryId(this.categoryId)
				.name(this.categoryName)
				.fullPath(this.categoryFullPath)
				.build())
			.owner(ResProductDetailDtoV1.OwnerInfo.builder()
				.ownerId(this.ownerId)
				.storeName(null)  // TODO: User 도메인 연동 시 추가
				.build())
			.options(this.options.stream()
				.map(opt -> ResProductDetailDtoV1.OptionInfo.builder()
					.optionId(opt.getOptionId())
					.name(opt.getName())
					.sortOrder(opt.getSortOrder())
					.values(opt.getValues().stream()
						.map(val -> ResProductDetailDtoV1.OptionValueInfo.builder()
							.optionValueId(val.getOptionValueId())
							.value(val.getValue())
							.build())
						.collect(Collectors.toList()))
					.build())
				.collect(Collectors.toList()))
			.variants(this.variants.stream()
				.map(var -> ResProductDetailDtoV1.VariantInfo.builder()
					.variantId(var.getVariantId())
					.skuCode(var.getSkuCode())
					.optionValueIds(var.getOptionValueIds())
					.optionName(var.getOptionName())
					.price(var.getPrice())
					.stockQuantity(var.getStockQuantity())
					.status(var.getStatus())
					.build())
				.collect(Collectors.toList()))
			.rating(ResProductDetailDtoV1.RatingInfo.builder()
				.avgRating(null)  // TODO: Review 도메인 연동 시 추가
				.reviewCount(null)
				.build())
			.createdAt(this.createdAt)
			.build();
	}

	private static String buildCategoryPath(Product product) {
		try {
			StringBuilder path = new StringBuilder();
			var current = product.getCategory();
			int maxDepth = 10;

			while (current != null && maxDepth-- > 0) {
				if (!path.isEmpty()) {
					path.insert(0, " > ");
				}
				path.insert(0, current.getName());
				current = current.getParent();
			}
			return path.toString();
		} catch (Exception e) {
			return product.getCategory().getName();
		}
	}
}