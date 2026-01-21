package com.groom.e_commerce.product.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.global.util.SecurityUtil;
import com.groom.e_commerce.product.application.dto.ProductCartInfo;
import com.groom.e_commerce.product.application.dto.StockManagement;
import com.groom.e_commerce.product.domain.entity.Category;
import com.groom.e_commerce.product.domain.entity.Product;
import com.groom.e_commerce.product.domain.entity.ProductOption;
import com.groom.e_commerce.product.domain.entity.ProductOptionValue;
import com.groom.e_commerce.product.domain.entity.ProductVariant;
import com.groom.e_commerce.product.domain.enums.ProductSortType;
import com.groom.e_commerce.product.domain.enums.ProductStatus;
import com.groom.e_commerce.product.domain.enums.VariantStatus;
import com.groom.e_commerce.product.domain.repository.ProductRepository;
import com.groom.e_commerce.product.domain.repository.ProductVariantRepository;
import com.groom.e_commerce.product.infrastructure.repository.ProductQueryRepository;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductSuspendDtoV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductUpdateDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductCreateDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductDetailDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductDtoV1;
import com.groom.e_commerce.product.infrastructure.cache.ProductDetailCacheService;
import com.groom.e_commerce.product.infrastructure.cache.ProductListCacheService;
import com.groom.e_commerce.product.infrastructure.cache.StockRedisService;
import com.groom.e_commerce.product.presentation.dto.response.ResProductListDtoV1;
import com.groom.e_commerce.product.presentation.dto.response.ResProductSearchDtoV1;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceV1 {

	private final ProductRepository productRepository;
	private final ProductVariantRepository productVariantRepository;
	private final ProductQueryRepository productQueryRepository;
	private final CategoryServiceV1 categoryService;
	private final ProductListCacheService listCacheService;
	private final ProductDetailCacheService detailCacheService;
	private final StockRedisService stockRedisService;

	/**
	 * 상품 등록 (Owner)
	 */
	@Transactional
	public ResProductCreateDtoV1 createProduct(ReqProductCreateDtoV1 request) {
		UUID ownerId = SecurityUtil.getCurrentUserId();

		Category category = categoryService.findActiveCategoryById(request.getCategoryId());

		// 옵션 존재 여부 결정
		boolean hasOptions = Boolean.TRUE.equals(request.getHasOptions())
			|| (request.getOptions() != null && !request.getOptions().isEmpty());

		Product product = Product.builder()
			.ownerId(ownerId)
			.category(category)
			.title(request.getTitle())
			.description(request.getDescription())
			.thumbnailUrl(request.getThumbnailUrl())
			.hasOptions(hasOptions)
			.price(request.getPrice())
			.stockQuantity(request.getStockQuantity())
			.build();

		// 옵션값 ID 목록 (variants에서 참조)
		List<List<UUID>> optionValueIdsList = new ArrayList<>();

		// 옵션 처리
		if (request.getOptions() != null && !request.getOptions().isEmpty()) {
			int optionSortOrder = 1;
			for (ReqProductCreateDtoV1.OptionRequest optionReq : request.getOptions()) {
				ProductOption option = ProductOption.builder()
					.product(product)
					.name(optionReq.getName())
					.sortOrder(optionReq.getSortOrder() != null ? optionReq.getSortOrder() : optionSortOrder++)
					.build();
				product.addOption(option);

				List<UUID> optionValueIds = new ArrayList<>();
				int valueSortOrder = 1;
				for (ReqProductCreateDtoV1.OptionValueRequest valueReq : optionReq.getValues()) {
					ProductOptionValue optionValue = ProductOptionValue.builder()
						.option(option)
						.value(valueReq.getValue())
						.sortOrder(valueReq.getSortOrder() != null ? valueReq.getSortOrder() : valueSortOrder++)
						.build();
					option.addOptionValue(optionValue);
					optionValueIds.add(null); // 나중에 저장 후 ID 할당됨
				}
				optionValueIdsList.add(optionValueIds);
			}
		}

		// 상품 저장 (옵션, 옵션값 Cascade 저장)
		Product savedProduct = productRepository.save(product);

		// Variants 처리
		if (request.getVariants() != null && !request.getVariants().isEmpty()) {
			// ... (기존 Variants 처리 로직 유지)
			List<List<UUID>> savedOptionValueIdsList = new ArrayList<>();
			for (ProductOption option : savedProduct.getOptions()) {
				List<UUID> valueIds = new ArrayList<>();
				for (ProductOptionValue value : option.getOptionValues()) {
					valueIds.add(value.getId());
				}
				savedOptionValueIdsList.add(valueIds);
			}

			for (ReqProductCreateDtoV1.VariantRequest variantReq : request.getVariants()) {
				if (variantReq.getSkuCode() != null && productVariantRepository.existsBySkuCode(variantReq.getSkuCode())) {
					throw new CustomException(ErrorCode.DUPLICATE_SKU_CODE);
				}

				List<UUID> optionValueIds = new ArrayList<>();
				String optionName = buildOptionName(variantReq.getOptionValueIndexes(), savedOptionValueIdsList,
					savedProduct.getOptions(), optionValueIds);

				ProductVariant variant = ProductVariant.builder()
					.product(savedProduct)
					.skuCode(variantReq.getSkuCode())
					.optionValueIds(optionValueIds)
					.optionName(optionName)
					.price(variantReq.getPrice())
					.stockQuantity(variantReq.getStockQuantity())
					.build();
				savedProduct.addVariant(variant);
			}
		}

		// [추가] Redis 재고 동기화 (Warm-up)
		if (Boolean.TRUE.equals(savedProduct.getHasOptions())) {
			savedProduct.getVariants().forEach(variant ->
				stockRedisService.syncStock(savedProduct.getId(), variant.getId(), variant.getStockQuantity())
			);
		} else {
			stockRedisService.syncStock(savedProduct.getId(), null, savedProduct.getStockQuantity());
		}

		// 캐시에 추가
		listCacheService.addProduct(savedProduct);
		detailCacheService.put(savedProduct);

		return ResProductCreateDtoV1.from(savedProduct);
	}

	/**
	 * optionValueIndexes를 기반으로 optionValueIds와 optionName 생성
	 */
	private String buildOptionName(
		List<Integer> optionValueIndexes,
		List<List<UUID>> savedOptionValueIdsList,
		List<ProductOption> options,
		List<UUID> outOptionValueIds
	) {
		if (optionValueIndexes == null || optionValueIndexes.isEmpty()) {
			return null;
		}

		StringBuilder nameBuilder = new StringBuilder();
		for (int i = 0; i < optionValueIndexes.size() && i < savedOptionValueIdsList.size(); i++) {
			int valueIndex = optionValueIndexes.get(i);
			List<UUID> valueIds = savedOptionValueIdsList.get(i);
			ProductOption option = options.get(i);

			if (valueIndex >= 0 && valueIndex < valueIds.size()) {
				UUID valueId = valueIds.get(valueIndex);
				outOptionValueIds.add(valueId);

				ProductOptionValue optionValue = option.getOptionValues().get(valueIndex);
				if (nameBuilder.length() > 0) {
					nameBuilder.append(" / ");
				}
				nameBuilder.append(optionValue.getValue());
			}
		}
		return nameBuilder.toString();
	}

	/**
	 * 내 상품 목록 조회 (Owner)
	 */
	public Page<ResProductListDtoV1> getSellerProducts(ProductStatus status, String keyword, Pageable pageable) {
		UUID ownerId = SecurityUtil.getCurrentUserId();

		Page<Product> products = productQueryRepository.findSellerProducts(ownerId, status, keyword, pageable);
		return products.map(ResProductListDtoV1::from);
	}

	/**
	 * 상품 수정 (Owner)
	 */
	@Transactional
	public ResProductDtoV1 updateProduct(UUID productId, ReqProductUpdateDtoV1 request) {
		UUID ownerId = SecurityUtil.getCurrentUserId();

		Product product = findProductById(productId);
		validateProductOwnership(product, ownerId);

		UUID oldCategoryId = product.getCategory().getId();

		Category category = null;
		if (request.getCategoryId() != null) {
			category = categoryService.findActiveCategoryById(request.getCategoryId());
		}

		product.update(
			category,
			request.getTitle(),
			request.getDescription(),
			request.getThumbnailUrl(),
			request.getPrice(),
			request.getStockQuantity(),
			request.getStatus()
		);

		// 캐시 업데이트
		detailCacheService.put(product);
		if (category != null && !category.getId().equals(oldCategoryId)) {
			// 카테고리 변경 시 목록 캐시 이동
			listCacheService.moveProduct(product, oldCategoryId);
		}

		return ResProductDtoV1.from(product);
	}

	/**
	 * 상품 삭제 - Soft Delete (Owner)
	 */
	@Transactional
	public void deleteProduct(UUID productId) {
		UUID ownerId = SecurityUtil.getCurrentUserId();

		Product product = findProductById(productId);
		validateProductOwnership(product, ownerId);

		UUID categoryId = product.getCategory().getId();
		product.softDelete(ownerId);

		// 캐시에서 제거
		listCacheService.removeProduct(productId, categoryId);
		detailCacheService.delete(productId);
	}

	/**
	 * 상품 조회 (삭제되지 않은 상품)
	 */
	public Product findProductById(UUID productId) {
		return productRepository.findByIdAndNotDeleted(productId)
			.orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
	}

	/**
	 * 상품 목록 조회 (구매자용 - 검색/필터링)
	 * 참고: 단순 목록 조회는 ProductReadService 사용 권장
	 */
	public Page<ResProductSearchDtoV1> searchProducts(
		UUID categoryId,
		String keyword,
		Long minPrice,
		Long maxPrice,
		ProductSortType sortType,
		Pageable pageable
	) {
		Page<Product> products = productQueryRepository.searchProductsForBuyer(
			keyword, categoryId, minPrice, maxPrice, sortType, pageable
		);
		return products.map(ResProductSearchDtoV1::from);
	}

	/**
	 * 상품 상세 조회 (구매자용 - 공개 API)
	 */
	@Transactional(readOnly = true)
	public ResProductDetailDtoV1 getProductDetail(UUID productId) {
		// Step 1: 상품 + 카테고리 조회
		Product product = productRepository.findByIdWithCategory(productId)
			.orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

		// 삭제된 상품은 조회 불가
		if (product.isDeleted()) {
			throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
		}

		// 판매중이 아닌 상품은 조회 불가 (구매자용)
		if (product.getStatus() != ProductStatus.ON_SALE) {
			throw new CustomException(ErrorCode.PRODUCT_NOT_ON_SALE);
		}

		// Step 2: 옵션 + 옵션값 조회 (같은 영속성 컨텍스트)
		productRepository.findByIdWithOptionsOnly(productId);

		// Step 3: variants 조회 (같은 영속성 컨텍스트)
		productRepository.findByIdWithVariantsOnly(productId);

		// TODO: Review 도메인에서 avgRating, reviewCount 조회
		// TODO: User 도메인에서 ownerStoreName 조회
		return ResProductDetailDtoV1.from(product, null, null, null);
	}

	/**
	 * 상품 목록 조회 (Manager)
	 */
	public Page<ResProductListDtoV1> getAllProductsForManager(String keyword, ProductStatus status, Pageable pageable) {
		Page<Product> products = productQueryRepository.findAllForManager(keyword, status, pageable);
		return products.map(ResProductListDtoV1::from);
	}

	/**
	 * 상품 정지 (Manager)
	 */
	@Transactional
	public ResProductDtoV1 suspendProduct(UUID productId, ReqProductSuspendDtoV1 request) {
		Product product = findProductById(productId);
		product.suspend(request.getReason());

		// 캐시에서 제거 (정지된 상품은 목록에서 미노출)
		listCacheService.removeProduct(productId, product.getCategory().getId());
		detailCacheService.delete(productId);

		return ResProductDtoV1.from(product);
	}

	/**
	 * 상품 정지 해제 (Manager)
	 */
	@Transactional
	public ResProductDtoV1 restoreProduct(UUID productId) {
		Product product = findProductById(productId);
		product.restore();

		// 캐시에 다시 추가
		listCacheService.addProduct(product);
		detailCacheService.put(product);

		return ResProductDtoV1.from(product);
	}

	/**
	 * 상품 소유권 검증
	 */
	private void validateProductOwnership(Product product, UUID ownerId) {
		if (!product.isOwnedBy(ownerId)) {
			throw new CustomException(ErrorCode.PRODUCT_ACCESS_DENIED);
		}
	}

	/**
	 * 장바구니 목록 조회를 위한 상품 정보 Bulk 조회
	 */
	public List<ProductCartInfo> getProductCartInfos(List<? extends StockManagement> items) {
		if (items == null || items.isEmpty()) {
			return new ArrayList<>();
		}

		// 1. ID 추출
		Set<UUID> productIds = items.stream()
			.map(StockManagement::getProductId)
			.collect(Collectors.toSet());

		Set<UUID> variantIds = items.stream()
			.filter(item -> item.getVariantId() != null)
			.map(StockManagement::getVariantId)
			.collect(Collectors.toSet());

		// 2. Bulk 조회 (삭제된 상품 제외, 빈 리스트 체크)
		Map<UUID, Product> productMap = productRepository.findByIdInAndNotDeleted(new ArrayList<>(productIds)).stream()
			.collect(Collectors.toMap(Product::getId, Function.identity()));

		Map<UUID, ProductVariant> variantMap = variantIds.isEmpty()
			? Map.of()
			: productVariantRepository.findByIdIn(new ArrayList<>(variantIds)).stream()
			.collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

		// 3. DTO 변환 및 로직 적용
		List<ProductCartInfo> result = new ArrayList<>();

		for (StockManagement item : items) {
			Product product = productMap.get(item.getProductId());
			if (product == null) {
				continue;
			}

			ProductVariant variant = null;
			if (item.getVariantId() != null) {
				variant = variantMap.get(item.getVariantId());
				// Variant가 없거나 해당 Product에 속하지 않으면 스킵
				if (variant == null || !variant.getProduct().getId().equals(product.getId())) {
					continue;
				}
			}

			// 판매 가능 여부: 상품 ON_SALE && (Variant 없거나 Variant ON_SALE)
			boolean isAvailable = (product.getStatus() == ProductStatus.ON_SALE)
				&& (variant == null || variant.getStatus() == VariantStatus.ON_SALE);

			result.add(ProductCartInfo.from(product, variant, isAvailable));
		}

		return result;
	}

	// ==================== 재고 관리 (Order 도메인 연동) ====================

	/**
	 * 단일 상품 재고 차감 (내부용 - 결제 확정 시 호출)
	 */
	@Transactional
	protected void decreaseStock(UUID productId, UUID variantId, int quantity) {
		if (variantId != null) {
			// 옵션 상품 차감
			ProductVariant variant = productVariantRepository.findById(variantId)
				.orElseThrow(() -> new CustomException(ErrorCode.VARIANT_NOT_FOUND));

			if (!variant.getProduct().getId().equals(productId)) {
				throw new CustomException(ErrorCode.VARIANT_NOT_FOUND);
			}

			variant.decreaseStock(quantity);
		} else {
			// 단일 상품 차감
			Product product = findProductById(productId);

			if (Boolean.TRUE.equals(product.getHasOptions())) {
				throw new CustomException(ErrorCode.VARIANT_REQUIRED);
			}

			product.decreaseStock(quantity);
		}
	}

	/**
	 * 재고 복원 (내부용 - 주문 취소/환불 확정 시 호출)
	 */
	@Transactional
	protected void increaseStock(UUID productId, UUID variantId, int quantity) {
		if (variantId != null) {
			ProductVariant variant = productVariantRepository.findById(variantId)
				.orElseThrow(() -> new CustomException(ErrorCode.VARIANT_NOT_FOUND));

			if (!variant.getProduct().getId().equals(productId)) {
				throw new CustomException(ErrorCode.VARIANT_NOT_FOUND);
			}

			variant.increaseStock(quantity);
		} else {
			Product product = findProductById(productId);

			if (Boolean.TRUE.equals(product.getHasOptions())) {
				throw new CustomException(ErrorCode.VARIANT_REQUIRED);
			}

			product.increaseStock(quantity);
		}
	}

	public ProductVariant findVariantById(UUID variantId) {
		return productVariantRepository.findById(variantId)
			.orElseThrow(() -> new CustomException(ErrorCode.VARIANT_NOT_FOUND));
	}

	// ==================== Redis 재고 관리 (가점유 시스템) ====================

	/**
	 * 재고 가점유 (Redis Lua Script - 주문서 작성 시)
	 * 원자적으로 재고 검증 + 차감
	 */
	public void reserveStock(UUID productId, UUID variantId, int quantity) {
		stockRedisService.reserve(productId, variantId, quantity);
	}

	/**
	 * Bulk 재고 가점유
	 */
	public void reserveStockBulk(List<StockManagement> items) {
		for (StockManagement item : items) {
			reserveStock(item.getProductId(), item.getVariantId(), item.getQuantity());
		}
	}

	/**
	 * 재고 가점유 해제 (Redis INCR - 주문 취소/타임아웃 시)
	 */
	public void releaseStock(UUID productId, UUID variantId, int quantity) {
		stockRedisService.release(productId, variantId, quantity);
	}

	/**
	 * Bulk 재고 가점유 해제
	 */
	public void releaseStockBulk(List<StockManagement> items) {
		for (StockManagement item : items) {
			releaseStock(item.getProductId(), item.getVariantId(), item.getQuantity());
		}
	}

	/**
	 * 재고 확정 차감 (결제 완료 시)
	 * DB 실재고 차감 + 상태 자동 변경 (ON_SALE/SOLD_OUT)
	 */
	@Transactional
	public void confirmStock(UUID productId, UUID variantId, int quantity) {
		// DB 실재고 차감 (엔티티 메서드 호출 → 상태 자동 변경)
		decreaseStock(productId, variantId, quantity);
	}

	/**
	 * Bulk 재고 확정 차감
	 */
	@Transactional
	public void confirmStockBulk(List<StockManagement> items) {
		for (StockManagement item : items) {
			confirmStock(item.getProductId(), item.getVariantId(), item.getQuantity());
		}
	}

	/**
	 * 재고 복구 (환불/취소 확정 시)
	 * Redis 재고 복구 + DB 재고 복구 + 상태 자동 변경
	 */
	@Transactional
	public void restoreStock(UUID productId, UUID variantId, int quantity) {
		// 1. Redis 가용 재고 복구
		stockRedisService.release(productId, variantId, quantity);

		// 2. DB 실재고 복구 (엔티티 메서드 호출 → 상태 자동 변경)
		increaseStock(productId, variantId, quantity);
	}

	/**
	 * Bulk 재고 복구
	 */
	@Transactional
	public void restoreStockBulk(List<StockManagement> items) {
		for (StockManagement item : items) {
			restoreStock(item.getProductId(), item.getVariantId(), item.getQuantity());
		}
	}

	/**
	 * 가용 재고 조회 (Redis)
	 */
	public Integer getAvailableStock(UUID productId, UUID variantId) {
		return stockRedisService.getAvailableStock(productId, variantId);
	}
}
