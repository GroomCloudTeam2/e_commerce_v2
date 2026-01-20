package com.groom.e_commerce.cart.domain.model;

import java.util.UUID;

/**
 * 장바구니 아이템의 논리적 식별자
 * (productId, variantId)
 */
public record CartItemKey(
    UUID productId,
    UUID variantId
) {}
