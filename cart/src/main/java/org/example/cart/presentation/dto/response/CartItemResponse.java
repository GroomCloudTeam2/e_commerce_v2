package org.example.cart.presentation.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * 장바구니 조회 응답 DTO
 */
@Getter
@Builder
public class CartItemResponse {

    // 식별자
    private UUID productId;
    private UUID variantId;

    // 상품 정보 (실시간 Product 서비스 기준)
    private String productName;
    private String optionName;
    private String thumbnailUrl;

    // 가격 정보
    private int price;        // 단가
    private int quantity;     // 장바구니 수량
    private int totalPrice;   // price * quantity

    // 재고 / 상태
    private int stockQuantity;
    private boolean isAvailable;
}
