package com.groom.e_commerce.product.application.service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.enums.ProductSortType;
import com.groom.e_commerce.product.domain.enums.ProductStatus;
import com.groom.e_commerce.product.domain.repository.ProductRepository;
import com.groom.e_commerce.product.infrastructure.cache.CachedProductDetail;
import com.groom.e_commerce.product.infrastructure.cache.ProductDetailCacheService;
import com.groom.e_commerce.product.infrastructure.cache.ProductListCacheService;
import com.groom.e_commerce.product.infrastructure.repository.ProductQueryRepository;
import com.groom.e_commerce.product.presentation.dto.response.ResProductDetailDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductSearchDtoV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 조회 전용 서비스 (Redis Read + DB Fallback)
 *
 * [조회 흐름]
 * 1. Redis 캐시 조회 (ZRANGE + MGET)
 * 2. 캐시 미스 시 DB 조회 → 캐시 적재 (Lazy Loading)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReadService {

	private final ProductListCacheService listCacheService;
	private final ProductDetailCacheService detailCacheService;
	private final ProductQueryRepository productQueryRepository;
	private final ProductRepository productRepository;
	private final ConcurrentHashMap<String, Object> mutexMap = new ConcurrentHashMap<>();

	/**
	 * 카테고리별 상품 목록 조회 (Redis + DB Fallback)
	 */
	@Transactional(readOnly = true)
	public Page<ResProductSearchDtoV1> getProductList(UUID categoryId, Pageable pageable) {
		int page = pageable.getPageNumber();
		int size = pageable.getPageSize();
		long offset = (long) page * size;

		// 1. ZRANGE로 ID 리스트 조회
		List<UUID> productIds = listCacheService.getProductIds(categoryId, offset, size);

		// 캐시 미스 → DB 조회 후 캐시 적재
		if (productIds.isEmpty()) {
			String lockKey = categoryId + ":" + page;
			synchronized (mutexMap.computeIfAbsent(lockKey, k -> new Object())) {
				// Double Check
				productIds = listCacheService.getProductIds(categoryId, offset, size);
				if (productIds.isEmpty()) {
					log.debug("Cache miss for product list: categoryId={}, page={}", categoryId, page);
					return loadListFromDbAndCache(categoryId, pageable);
				}
			}
		}

		// 2. MGET으로 상세 일괄 조회
		List<CachedProductDetail> details = detailCacheService.multiGetAsList(productIds);

		// 3. DTO 변환 (null 제외)
		List<ResProductSearchDtoV1> content = details.stream()
			.filter(detail -> detail != null)
			.map(this::toSearchDto)
			.toList();

		// 4. 전체 개수 조회
		long totalCount = listCacheService.getCachedTotalCount(categoryId);
		if (totalCount == -1) {
			totalCount = productQueryRepository.countProductsForBuyer(categoryId);
			listCacheService.setTotalCount(categoryId, totalCount);
		}

		return new PageImpl<>(content, pageable, totalCount);
	}

	/**
	 * 상품 상세 조회 (Redis + DB Fallback)
	 */
	@Transactional(readOnly = true)
	public ResProductDetailDtoV1 getProductDetail(UUID productId) {
		CachedProductDetail cached = detailCacheService.get(productId);

		// 캐시 미스 → DB 조회 후 캐시 적재
		if (cached == null) {
			log.debug("Cache miss for product detail: productId={}", productId);
			return loadDetailFromDbAndCache(productId);
		}

		return cached.toResponseDto();
	}

	/**
	 * DB에서 목록 조회 후 캐시 적재 (Lazy Loading)
	 */
	private Page<ResProductSearchDtoV1> loadListFromDbAndCache(UUID categoryId, Pageable pageable) {
		// DB 조회
		Page<Product> products = productQueryRepository.searchProductsForBuyer(
			null, categoryId, null, null, ProductSortType.NEWEST, pageable
		);

		// Total Count 캐싱
		listCacheService.setTotalCount(categoryId, products.getTotalElements());

		if (products.isEmpty()) {
			return Page.empty(pageable);
		}

		// 캐시 적재 (비동기로 처리해도 됨)
		products.forEach(product -> {
			listCacheService.addProduct(product);
			detailCacheService.put(product);
		});

		log.info("Loaded {} products from DB and cached: categoryId={}", products.getNumberOfElements(), categoryId);

		// DTO 변환
		return products.map(ResProductSearchDtoV1::from);
	}

	/**
	 * DB에서 상세 조회 후 캐시 적재 (Lazy Loading)
	 */
	private ResProductDetailDtoV1 loadDetailFromDbAndCache(UUID productId) {
		// DB 조회 (옵션, Variant 포함)
		Product product = productRepository.findByIdWithCategory(productId).orElse(null);

		if (product == null || product.isDeleted()) {
			return null;
		}

		// 판매중이 아닌 상품은 조회 불가
		if (product.getStatus() != ProductStatus.ON_SALE) {
			return null;
		}

		// 옵션, Variant 로딩
		productRepository.findByIdWithOptionsOnly(productId);
		productRepository.findByIdWithVariantsOnly(productId);

		// 캐시 적재
		listCacheService.addProduct(product);
		detailCacheService.put(product);

		log.info("Loaded product from DB and cached: productId={}", productId);

		return ResProductDetailDtoV1.from(product, null, null, null);
	}

	/**
	 * 캐시 존재 여부 확인
	 */
	public boolean existsInCache(UUID productId) {
		return detailCacheService.exists(productId);
	}

	private ResProductSearchDtoV1 toSearchDto(CachedProductDetail detail) {
		return ResProductSearchDtoV1.builder()
			.productId(detail.getProductId())
			.title(detail.getTitle())
			.thumbnailUrl(detail.getThumbnailUrl())
			.status(detail.getStatus())
			.minPrice(detail.getMinPrice())
			.maxPrice(detail.getMaxPrice())
			.avgRating(null)
			.reviewCount(null)
			.categoryName(detail.getCategoryName())
			.ownerStoreName(null)
			.build();
	}
}