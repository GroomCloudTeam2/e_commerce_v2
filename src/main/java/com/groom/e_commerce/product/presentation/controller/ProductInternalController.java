package com.groom.e_commerce.product.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groom.e_commerce.product.application.dto.ProductCartInfo;
import com.groom.e_commerce.product.application.dto.StockManagement;
import com.groom.e_commerce.product.application.service.ProductServiceV1;
import com.groom.e_commerce.product.presentation.dto.request.ReqProductBulkInfoDto;
import com.groom.e_commerce.product.presentation.dto.request.ReqStockOperationDto;
import com.groom.e_commerce.product.presentation.dto.response.ResProductBulkInfoDto;
import com.groom.e_commerce.product.presentation.dto.response.ResStockAvailabilityDto;
import com.groom.e_commerce.product.presentation.dto.response.ResStockOperationDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Product Internal API Controller
 * MSA 환경에서 다른 서비스(Order, Cart)가 OpenFeign을 통해 호출하는 내부 API
 *
 * 보안: API Gateway에서 내부 서비스 간 통신만 허용하도록 설정 필요
 */
@Slf4j
@Tag(name = "Product Internal", description = "상품 내부 API (서비스 간 통신용)")
@RestController
@RequestMapping("/api/v1/internal/products")
@RequiredArgsConstructor
public class ProductInternalController {

	private final ProductServiceV1 productService;

	// ==================== 재고 관리 API ====================

	/**
	 * 재고 가점유 (Reserve)
	 * - 호출 시점: 주문서 생성 시 (Order 서비스)
	 * - 동작: Redis에서 원자적으로 재고 검증 + 차감
	 */
	@Operation(summary = "재고 가점유", description = "주문 생성 시 재고를 가점유합니다. (Redis Lua Script)")
	@PostMapping("/stock/reserve")
	public ResponseEntity<ResStockOperationDto> reserveStock(
		@Valid @RequestBody ReqStockOperationDto request
	) {
		log.info("[Internal API] 재고 가점유 요청 - items: {}", request.getItems().size());

		List<StockManagement> stockManagements = request.getItems().stream()
			.map(item -> StockManagement.of(
				item.getProductId(),
				item.getVariantId(),
				item.getQuantity()
			))
			.toList();

		productService.reserveStockBulk(stockManagements);

		log.info("[Internal API] 재고 가점유 완료");
		return ResponseEntity.ok(ResStockOperationDto.success("재고 가점유가 완료되었습니다."));
	}

	/**
	 * 가용 재고 조회
	 * - 호출 시점: 재고 확인이 필요할 때
	 * - 동작: Redis에서 현재 가용 재고 조회
	 */
	@Operation(summary = "가용 재고 조회", description = "Redis에서 현재 가용 재고를 조회합니다.")
	@GetMapping("/{productId}/stock")
	public ResponseEntity<ResStockAvailabilityDto> getAvailableStock(
		@PathVariable UUID productId
	) {
		Integer stock = productService.getAvailableStock(productId, null);
		return ResponseEntity.ok(ResStockAvailabilityDto.of(productId, null, stock));
	}

	/**
	 * Variant 가용 재고 조회
	 */
	@Operation(summary = "Variant 가용 재고 조회", description = "옵션 상품의 가용 재고를 조회합니다.")
	@GetMapping("/{productId}/variants/{variantId}/stock")
	public ResponseEntity<ResStockAvailabilityDto> getVariantAvailableStock(
		@PathVariable UUID productId,
		@PathVariable UUID variantId
	) {
		Integer stock = productService.getAvailableStock(productId, variantId);
		return ResponseEntity.ok(ResStockAvailabilityDto.of(productId, variantId, stock));
	}

	// ==================== 상품 정보 조회 API ====================

	/**
	 * 상품 정보 벌크 조회
	 * - 호출 시점: 주문 생성, 장바구니 조회 시 (Order/Cart 서비스)
	 * - 동작: N+1 방지를 위한 벌크 조회
	 */
	@Operation(summary = "상품 정보 벌크 조회", description = "여러 상품 정보를 한 번에 조회합니다. (N+1 방지)")
	@PostMapping("/bulk-info")
	public ResponseEntity<ResProductBulkInfoDto> getProductBulkInfo(
		@Valid @RequestBody ReqProductBulkInfoDto request
	) {
		log.info("[Internal API] 상품 정보 벌크 조회 요청 - items: {}", request.getItems().size());

		List<StockManagement> stockManagements = request.getItems().stream()
			.map(item -> StockManagement.of(
				item.getProductId(),
				item.getVariantId(),
				item.getQuantity()
			))
			.toList();

		List<ProductCartInfo> productInfos = productService.getProductCartInfos(stockManagements);

		log.info("[Internal API] 상품 정보 벌크 조회 완료 - results: {}", productInfos.size());
		return ResponseEntity.ok(ResProductBulkInfoDto.from(productInfos));
	}
}