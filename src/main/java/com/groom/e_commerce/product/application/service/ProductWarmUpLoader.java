package com.groom.e_commerce.product.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.repository.ProductRepository;
import com.groom.e_commerce.product.infrastructure.cache.ProductDetailCacheService;
import com.groom.e_commerce.product.infrastructure.cache.ProductListCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductWarmUpLoader {

	private final ProductRepository productRepository;
	private final ProductListCacheService listCacheService;
	private final ProductDetailCacheService detailCacheService;

	// 메모리 보호를 위해 최신 10만 개만 Warm-up
	private static final int WARM_UP_LIMIT = 100000;
	private static final int PAGE_SIZE = 1000;

	@Transactional(readOnly = true)
	public void loadTopProducts() {
		log.info("Starting Product Cache Warm-up (Limit: {})...", WARM_UP_LIMIT);
		
		int totalPages = (int) Math.ceil((double) WARM_UP_LIMIT / PAGE_SIZE);

		for (int i = 0; i < totalPages; i++) {
			Page<Product> products = productRepository.findAll(
				PageRequest.of(i, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
			);
			
			if (products.isEmpty()) break;

			for (Product product : products) {
				// 1. 상세 캐시 (JSON) 적재
				detailCacheService.put(product);
				
				// 2. 목록 캐시 (ZSET) 적재 (판매 중/품절 상태만)
				if (product.getStatus() == com.groom.e_commerce.product.domain.enums.ProductStatus.ON_SALE || 
					product.getStatus() == com.groom.e_commerce.product.domain.enums.ProductStatus.SOLD_OUT) {
					listCacheService.addProduct(product);
				}
			}
			
			log.info("Warm-up Progress: {} products cached...", (i + 1) * PAGE_SIZE);
		}
		
		log.info("Product Cache Warm-up Completed successfully.");
	}
}
