package com.groom.e_commerce.product.infrastructure.cache;

import java.util.UUID;

/**
 * 재고 Redis 키 관리
 *
 * 키 구조:
 * - 옵션 없는 상품: stock:product:{productId}
 * - 옵션 있는 상품: stock:variant:{variantId}
 * - 예약 정보: stock:reservation:{reservationId}
 */
public final class StockCacheKey {

	private static final String STOCK_PRODUCT_PREFIX = "stock:product:";
	private static final String STOCK_VARIANT_PREFIX = "stock:variant:";
	private static final String RESERVATION_PREFIX = "stock:reservation:";

	private StockCacheKey() {
	}

	/**
	 * 옵션 없는 상품의 재고 키
	 */
	public static String productStock(UUID productId) {
		return STOCK_PRODUCT_PREFIX + productId.toString();
	}

	/**
	 * 옵션 있는 상품(Variant)의 재고 키
	 */
	public static String variantStock(UUID variantId) {
		return STOCK_VARIANT_PREFIX + variantId.toString();
	}

	/**
	 * 상품 유형에 따른 재고 키 자동 선택
	 * @param productId 상품 ID (필수)
	 * @param variantId Variant ID (옵션 상품인 경우)
	 */
	public static String stockKey(UUID productId, UUID variantId) {
		if (variantId != null) {
			return variantStock(variantId);
		}
		return productStock(productId);
	}

	/**
	 * 예약(가점유) 정보 키
	 */
	public static String reservation(String reservationId) {
		return RESERVATION_PREFIX + reservationId;
	}

	/**
	 * 모든 상품 재고 키 패턴 (동기화용)
	 */
	public static String allProductStockPattern() {
		return STOCK_PRODUCT_PREFIX + "*";
	}

	/**
	 * 모든 Variant 재고 키 패턴 (동기화용)
	 */
	public static String allVariantStockPattern() {
		return STOCK_VARIANT_PREFIX + "*";
	}
}
