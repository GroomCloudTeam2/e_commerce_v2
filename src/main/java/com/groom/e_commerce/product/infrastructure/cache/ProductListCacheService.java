package com.groom.e_commerce.product.infrastructure.cache;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.groom.e_commerce.product.domain.entity.Product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 목록 Redis Sorted Set 캐시 서비스
 *
 * 키: product:list:category:{categoryId}
 * 값: Sorted Set (score: createdAt timestamp, member: productId)
 *
 * 장점:
 * - 상품 변경 시 해당 상품만 추가/삭제 (목록 전체 무효화 불필요)
 * - ZREVRANGE로 페이지네이션 즉시 처리
 * - 키 수 = 카테고리 수 (키 폭발 방지)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductListCacheService {

	private final StringRedisTemplate stringRedisTemplate;

	/**
	 * 카테고리 목록에 상품 추가
	 * @param product 추가할 상품
	 */
	public void addProduct(Product product) {
		try {
			String productId = product.getId().toString();
			double score = product.getCreatedAt().toEpochSecond(ZoneOffset.UTC);

			// 카테고리별 목록에 추가
			String categoryKey = ProductCacheKey.productList(product.getCategory().getId());
			stringRedisTemplate.opsForZSet().add(categoryKey, productId, score);

			// 전체 목록에도 추가
			String allKey = ProductCacheKey.productListAll();
			stringRedisTemplate.opsForZSet().add(allKey, productId, score);

			log.debug("Product added to list cache: productId={}, categoryId={}",
				productId, product.getCategory().getId());
		} catch (Exception e) {
			log.error("Failed to add product to list cache: productId={}", product.getId(), e);
		}
	}

	/**
	 * 카테고리 목록에서 상품 제거
	 * @param productId 제거할 상품 ID
	 * @param categoryId 카테고리 ID
	 */
	public void removeProduct(UUID productId, UUID categoryId) {
		try {
			String productIdStr = productId.toString();

			// 카테고리별 목록에서 제거
			String categoryKey = ProductCacheKey.productList(categoryId);
			stringRedisTemplate.opsForZSet().remove(categoryKey, productIdStr);

			// 전체 목록에서도 제거
			String allKey = ProductCacheKey.productListAll();
			stringRedisTemplate.opsForZSet().remove(allKey, productIdStr);

			log.debug("Product removed from list cache: productId={}, categoryId={}", productId, categoryId);
		} catch (Exception e) {
			log.error("Failed to remove product from list cache: productId={}", productId, e);
		}
	}

	/**
	 * 상품의 카테고리 변경 처리
	 * @param product 변경된 상품
	 * @param oldCategoryId 이전 카테고리 ID
	 */
	public void moveProduct(Product product, UUID oldCategoryId) {
		try {
			String productId = product.getId().toString();
			double score = product.getCreatedAt().toEpochSecond(ZoneOffset.UTC);

			// 이전 카테고리에서 제거
			String oldKey = ProductCacheKey.productList(oldCategoryId);
			stringRedisTemplate.opsForZSet().remove(oldKey, productId);

			// 새 카테고리에 추가
			String newKey = ProductCacheKey.productList(product.getCategory().getId());
			stringRedisTemplate.opsForZSet().add(newKey, productId, score);

			log.debug("Product moved in list cache: productId={}, from={}, to={}",
				productId, oldCategoryId, product.getCategory().getId());
		} catch (Exception e) {
			log.error("Failed to move product in list cache: productId={}", product.getId(), e);
		}
	}

	/**
	 * 카테고리별 상품 ID 목록 조회 (최신순)
	 * @param categoryId 카테고리 ID (null이면 전체)
	 * @param offset 시작 위치 (0부터)
	 * @param limit 조회 개수
	 * @return 상품 ID 리스트
	 */
	public List<UUID> getProductIds(UUID categoryId, long offset, long limit) {
		try {
			String key = ProductCacheKey.productList(categoryId);

			// ZREVRANGE: score 내림차순 (최신순)
			Set<String> productIds = stringRedisTemplate.opsForZSet()
				.reverseRange(key, offset, offset + limit - 1);

			if (productIds == null || productIds.isEmpty()) {
				return Collections.emptyList();
			}

			return productIds.stream()
				.map(UUID::fromString)
				.toList();
		} catch (Exception e) {
			log.error("Failed to get product IDs from cache: categoryId={}", categoryId, e);
			return Collections.emptyList();
		}
	}

	/**
	 * 카테고리별 전체 상품 수 조회 (ZSet 크기 반환)
	 * @deprecated getCachedTotalCount 사용 권장
	 */
	@Deprecated
	public long getTotalCount(UUID categoryId) {
		try {
			String key = ProductCacheKey.productList(categoryId);
			Long count = stringRedisTemplate.opsForZSet().zCard(key);
			return count != null ? count : 0;
		} catch (Exception e) {
			log.error("Failed to get total count from cache: categoryId={}", categoryId, e);
			return 0;
		}
	}

	/**
	 * 전체 상품 수 캐시 조회
	 * @return -1 if cache miss
	 */
	public long getCachedTotalCount(UUID categoryId) {
		try {
			String key = ProductCacheKey.productCount(categoryId);
			String val = stringRedisTemplate.opsForValue().get(key);
			return val != null ? Long.parseLong(val) : -1;
		} catch (Exception e) {
			log.error("Failed to get cached total count: categoryId={}", categoryId, e);
			return -1;
		}
	}

	/**
	 * 전체 상품 수 캐시 설정
	 */
	public void setTotalCount(UUID categoryId, long count) {
		try {
			String key = ProductCacheKey.productCount(categoryId);
			stringRedisTemplate.opsForValue().set(key, String.valueOf(count));
		} catch (Exception e) {
			log.error("Failed to set total count cache: categoryId={}", categoryId, e);
		}
	}

	/**
	 * 전체 상품 수 캐시 삭제 (CUD 발생 시 호출)
	 */
	public void deleteTotalCount(UUID categoryId) {
		try {
			String key = ProductCacheKey.productCount(categoryId);
			stringRedisTemplate.delete(key);
		} catch (Exception e) {
			log.error("Failed to delete total count cache: categoryId={}", categoryId, e);
		}
	}

	/**
	 * 카테고리 목록 캐시 전체 삭제
	 */
	public void clearCategory(UUID categoryId) {
		try {
			String key = ProductCacheKey.productList(categoryId);
			stringRedisTemplate.delete(key);
			log.debug("Category list cache cleared: categoryId={}", categoryId);
		} catch (Exception e) {
			log.error("Failed to clear category list cache: categoryId={}", categoryId, e);
		}
	}

	/**
	 * 전체 목록 캐시 삭제
	 */
	public void clearAll() {
		try {
			Set<String> keys = stringRedisTemplate.keys(ProductCacheKey.productListPattern());
			if (keys != null && !keys.isEmpty()) {
				stringRedisTemplate.delete(keys);
				log.debug("All list cache cleared: {} keys", keys.size());
			}
		} catch (Exception e) {
			log.error("Failed to clear all list cache", e);
		}
	}
}