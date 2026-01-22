package com.groom.e_commerce.product.infrastructure.cache;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.groom.e_commerce.product.domain.entity.Product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 상세 Redis 캐시 서비스
 *
 * 키: product:detail:{productId}
 * 값: CachedProductDetail (JSON)
 *
 * 특징:
 * - TTL 없음 (이벤트 기반 즉시 업데이트)
 * - MGET으로 다수 상품 일괄 조회 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductDetailCacheService {

	private final RedisTemplate<String, Object> redisTemplate;

	/**
	 * 상품 상세 캐시 저장/업데이트
	 * @param product 저장할 상품 엔티티
	 */
	public void put(Product product) {
		try {
			String key = ProductCacheKey.productDetail(product.getId());
			CachedProductDetail cached = CachedProductDetail.from(product);

			redisTemplate.opsForValue().set(key, cached);

			log.debug("Product detail cached: productId={}", product.getId());
		} catch (Exception e) {
			log.error("Failed to cache product detail: productId={}", product.getId(), e);
		}
	}

	/**
	 * 상품 상세 캐시 조회
	 * @param productId 상품 ID
	 * @return 캐시된 상품 상세 (없으면 null)
	 */
	public CachedProductDetail get(UUID productId) {
		try {
			String key = ProductCacheKey.productDetail(productId);
			Object cached = redisTemplate.opsForValue().get(key);

			if (cached instanceof CachedProductDetail) {
				log.debug("Product detail cache hit: productId={}", productId);
				return (CachedProductDetail) cached;
			}

			log.debug("Product detail cache miss: productId={}", productId);
			return null;
		} catch (Exception e) {
			log.error("Failed to get product detail from cache: productId={}", productId, e);
			return null;
		}
	}

	/**
	 * 다수 상품 상세 일괄 조회 (MGET)
	 * @param productIds 상품 ID 리스트
	 * @return productId -> CachedProductDetail 맵 (캐시 미스는 포함되지 않음)
	 */
	public Map<UUID, CachedProductDetail> multiGet(List<UUID> productIds) {
		if (productIds == null || productIds.isEmpty()) {
			return Collections.emptyMap();
		}

		try {
			List<String> keys = productIds.stream()
				.map(ProductCacheKey::productDetail)
				.toList();

			List<Object> results = redisTemplate.opsForValue().multiGet(keys);

			if (results == null) {
				return Collections.emptyMap();
			}

			// 결과 매핑 (null 제외)
			Map<UUID, CachedProductDetail> resultMap = new java.util.HashMap<>();
			for (int i = 0; i < productIds.size(); i++) {
				Object cached = results.get(i);
				if (cached instanceof CachedProductDetail detail) {
					resultMap.put(productIds.get(i), detail);
				}
			}

			log.debug("Product detail multi-get: requested={}, found={}",
				productIds.size(), resultMap.size());

			return resultMap;
		} catch (Exception e) {
			log.error("Failed to multi-get product details from cache", e);
			return Collections.emptyMap();
		}
	}

	/**
	 * 다수 상품 상세를 리스트로 조회 (순서 유지, 캐시 미스는 null)
	 * @param productIds 상품 ID 리스트
	 * @return CachedProductDetail 리스트 (순서 유지)
	 */
	public List<CachedProductDetail> multiGetAsList(List<UUID> productIds) {
		if (productIds == null || productIds.isEmpty()) {
			return Collections.emptyList();
		}

		try {
			List<String> keys = productIds.stream()
				.map(ProductCacheKey::productDetail)
				.toList();

			List<Object> results = redisTemplate.opsForValue().multiGet(keys);

			if (results == null) {
				return Collections.emptyList();
			}

			return results.stream()
				.map(obj -> obj instanceof CachedProductDetail ? (CachedProductDetail) obj : null)
				.toList();
		} catch (Exception e) {
			log.error("Failed to multi-get product details as list from cache", e);
			return Collections.emptyList();
		}
	}

	/**
	 * 상품 상세 캐시 삭제
	 * @param productId 삭제할 상품 ID
	 */
	public void delete(UUID productId) {
		try {
			String key = ProductCacheKey.productDetail(productId);
			redisTemplate.delete(key);

			log.debug("Product detail cache deleted: productId={}", productId);
		} catch (Exception e) {
			log.error("Failed to delete product detail from cache: productId={}", productId, e);
		}
	}

	/**
	 * 캐시 존재 여부 확인
	 * @param productId 상품 ID
	 */
	public boolean exists(UUID productId) {
		try {
			String key = ProductCacheKey.productDetail(productId);
			Boolean exists = redisTemplate.hasKey(key);
			return Boolean.TRUE.equals(exists);
		} catch (Exception e) {
			log.error("Failed to check product detail cache existence: productId={}", productId, e);
			return false;
		}
	}

	/**
	 * 전체 상품 상세 캐시 삭제 (주의: 운영 환경에서 신중히 사용)
	 */
	public void clearAll() {
		try {
			Set<String> keys = redisTemplate.keys(ProductCacheKey.productDetailPattern());
			if (keys != null && !keys.isEmpty()) {
				redisTemplate.delete(keys);
				log.info("All product detail cache cleared: {} keys", keys.size());
			}
		} catch (Exception e) {
			log.error("Failed to clear all product detail cache", e);
		}
	}
}
