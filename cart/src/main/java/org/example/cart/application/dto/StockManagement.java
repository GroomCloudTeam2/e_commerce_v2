package org.example.cart.application.dto;

import java.util.UUID;

/**
 * 상품 서비스에 전달하는 재고 검증 요청 모델
 */
public record StockManagement(
    UUID productId,
    UUID variantId,
    int quantity
) {
    public static StockManagement of(
        UUID productId,
        UUID variantId,
        int quantity
    ) {
        return new StockManagement(productId, variantId, quantity);
    }
}
