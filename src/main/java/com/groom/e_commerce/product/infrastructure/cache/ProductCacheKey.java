package com.groom.e_commerce.product.infrastructure.cache;

import java.util.UUID;

/**
 * Product 도메인 Redis 캐시 키 전략
 *
 * [목록 캐시 - Sorted Set]
 * product:list:category:{categoryId} → score: createdAt(Timestamp), member: productId
 *
 * [상세 캐시 - String(JSON)]
 * product:detail:{productId} → JSON (옵션, Variant 포함)
 */
public final class ProductCacheKey {

	// ==================== 상품 목록 (Sorted Set) ====================
	private static final String LIST_PREFIX = "product:list:category:";
	private static final String LIST_ALL = "product:list:all";

	// ==================== 상품 개수 (String) ====================
	private static final String COUNT_PREFIX = "product:count:category:";

	// ==================== 상품 상세 (String/JSON) ====================
	private static final String DETAIL_PREFIX = "product:detail:";

	private ProductCacheKey() {
	}

	// ==================== 목록 키 ====================

	/**
	 * 카테고리별 상품 목록 키 (Sorted Set)
	 * @param categoryId 카테고리 ID (null이면 전체)
	 */
	public static String productList(UUID categoryId) {
		if (categoryId == null) {
			return LIST_ALL;
		}
		return LIST_PREFIX + categoryId.toString();
	}

	/**
	 * 전체 상품 목록 키
	 */
	public static String productListAll() {
		return LIST_ALL;
	}

	// ==================== 개수 키 ====================
	public static String productCount(UUID categoryId) {
		if (categoryId == null) {
			return COUNT_PREFIX + "all";
		}
		return COUNT_PREFIX + categoryId.toString();
	}

	// ==================== 상세 키 ====================

	/**
	 * 상품 상세 캐시 키
	 * @param productId 상품 ID
	 */
	public static String productDetail(UUID productId) {
		return DETAIL_PREFIX + productId.toString();
	}

	// ==================== 패턴 (일괄 삭제용) ====================

	/**
	 * 상품 목록 키 패턴 (전체 삭제용)
	 */
	public static String productListPattern() {
		return LIST_PREFIX + "*";
	}

	/**
	 * 상품 상세 키 패턴 (전체 삭제용)
	 */
	public static String productDetailPattern() {
		return DETAIL_PREFIX + "*";
	}
}