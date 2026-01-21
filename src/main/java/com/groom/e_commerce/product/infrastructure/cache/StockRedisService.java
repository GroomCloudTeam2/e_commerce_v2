package com.groom.e_commerce.product.infrastructure.cache;

import java.util.Collections;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockRedisService {

	private final StringRedisTemplate stringRedisTemplate;

	private DefaultRedisScript<Long> reserveScript;

	@PostConstruct
	public void init() {
		reserveScript = new DefaultRedisScript<>();
		reserveScript.setScriptSource(new ResourceScriptSource(
			new ClassPathResource("scripts/stock_reserve.lua")));
		reserveScript.setResultType(Long.class);
	}

	/**
	 * 재고 가점유 (Lua Script - 원자적 검증 + 차감)
	 *
	 * @param productId 상품 ID
	 * @param variantId Variant ID (옵션 없으면 null)
	 * @param quantity 차감할 수량
	 * @return true: 성공, false: 실패
	 * @throws CustomException 재고 부족 또는 키 없음
	 */
	public boolean reserve(UUID productId, UUID variantId, int quantity) {
		String key = StockCacheKey.stockKey(productId, variantId);

		Long result = stringRedisTemplate.execute(
			reserveScript,
			Collections.singletonList(key),
			String.valueOf(quantity)
		);

		if (result == null || result == -1) {
			log.warn("Stock key not found: {}", key);
			throw new CustomException(ErrorCode.STOCK_NOT_ENOUGH);
		}

		if (result == 0) {
			log.info("Stock not enough for reserve: key={}, requested={}", key, quantity);
			throw new CustomException(ErrorCode.STOCK_NOT_ENOUGH);
		}

		log.debug("Stock reserved: key={}, quantity={}", key, quantity);
		return true;
	}

	/**
	 * 재고 복원 (원자적 INCR)
	 *
	 * @param productId 상품 ID
	 * @param variantId Variant ID (옵션 없으면 null)
	 * @param quantity 복원할 수량
	 */
	public void release(UUID productId, UUID variantId, int quantity) {
		String key = StockCacheKey.stockKey(productId, variantId);

		try {
			stringRedisTemplate.opsForValue().increment(key, quantity);
			log.debug("Stock released: key={}, quantity={}", key, quantity);
		} catch (Exception e) {
			log.error("Failed to release stock: key={}, quantity={}", key, quantity, e);
		}
	}

	/**
	 * 현재 가용 재고 조회
	 */
	public Integer getAvailableStock(UUID productId, UUID variantId) {
		String key = StockCacheKey.stockKey(productId, variantId);

		try {
			String value = stringRedisTemplate.opsForValue().get(key);
			if (value == null) {
				return null;
			}
			return Integer.parseInt(value);
		} catch (Exception e) {
			log.error("Failed to get stock: key={}", key, e);
			return null;
		}
	}

	/**
	 * 재고 동기화 (DB → Redis)
	 */
	public void syncStock(UUID productId, UUID variantId, int stockQuantity) {
		String key = StockCacheKey.stockKey(productId, variantId);

		try {
			stringRedisTemplate.opsForValue().set(key, String.valueOf(stockQuantity));
			log.debug("Stock synced: key={}, quantity={}", key, stockQuantity);
		} catch (Exception e) {
			log.error("Failed to sync stock: key={}, quantity={}", key, stockQuantity, e);
		}
	}

	/**
	 * 재고 키 삭제 (상품 삭제 시)
	 */
	public void deleteStock(UUID productId, UUID variantId) {
		String key = StockCacheKey.stockKey(productId, variantId);

		try {
			stringRedisTemplate.delete(key);
			log.debug("Stock deleted: key={}", key);
		} catch (Exception e) {
			log.error("Failed to delete stock: key={}", key, e);
		}
	}
}
