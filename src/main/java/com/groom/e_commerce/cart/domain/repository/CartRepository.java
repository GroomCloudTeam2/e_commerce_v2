package com.groom.e_commerce.cart.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.groom.e_commerce.cart.domain.model.CartItem;
import com.groom.e_commerce.cart.domain.model.CartItemKey;

public interface CartRepository {

    /** 장바구니 상품 추가 (중복 시 수량 증가) */
    void addItem(UUID userId, UUID productId, UUID variantId, int quantity);

    /** 단일 아이템 조회 */
    Optional<CartItem> findItem(UUID userId, UUID productId, UUID variantId);

    /** 장바구니 전체 조회 */
    List<CartItem> findAll(UUID userId);

    /** 수량 변경 (덮어쓰기) */
    void updateQuantity(UUID userId, UUID productId, UUID variantId, int quantity);

    /** 단일 아이템 삭제 */
    void removeItem(UUID userId, UUID productId, UUID variantId);

    /** 여러 아이템 삭제 */
    void removeItems(UUID userId, List<CartItemKey> keys);

    /** 장바구니 전체 삭제 */
    void clear(UUID userId);
}
