package com.groom.e_commerce.cart.infrastructure.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.groom.e_commerce.cart.application.dto.ProductCartInfo;
import com.groom.e_commerce.cart.application.dto.StockManagement;

/**
 * Product 서비스 API 호출용 Feign Client
 *
 * Cart 서비스 입장에서:
 * - 상품 존재 여부
 * - 판매 가능 여부
 * - 재고 수량
 * 만 조회한다
 */
@FeignClient(
    name = "product-service",
    url = "${external.product-service.url}",
    fallback = ProductClientFallback.class
)
public interface ProductClient {

    @PostMapping("/internal/api/v1/products/cart")
    List<ProductCartInfo> getProductCartInfos(
        @RequestBody List<StockManagement> requests
    );
}
