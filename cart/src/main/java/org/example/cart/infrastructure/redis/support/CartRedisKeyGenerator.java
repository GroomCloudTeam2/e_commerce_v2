package org.example.cart.infrastructure.redis.support;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class CartRedisKeyGenerator {

    private static final String CART_PREFIX = "cart:";
    private static final long CART_TTL_DAYS = 30L;

    /**
     * cart:{userId}
     */
    public String cartKey(UUID userId) {
        return CART_PREFIX + userId;
    }

    /**
     * productId:variantId
     */
    public String itemField(UUID productId, UUID variantId) {
        return productId + ":" + variantId;
    }

    public long ttlDays() {
        return CART_TTL_DAYS;
    }
}
