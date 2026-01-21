package com.groom.e_commerce.cart.presentation.dto.request;

import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public class CartAddRequest {

    private UUID productId;
    private UUID variantId;
    private int quantity;

    // 테스트 / 내부 생성용
    public CartAddRequest(UUID productId, UUID variantId, int quantity) {
        this.productId = productId;
        this.variantId = variantId;
        this.quantity = quantity;
    }
}
