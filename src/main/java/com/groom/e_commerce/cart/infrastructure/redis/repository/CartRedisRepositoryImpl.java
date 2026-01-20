package com.groom.e_commerce.cart.infrastructure.redis.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.groom.e_commerce.cart.domain.dto.CartItem;
import com.groom.e_commerce.cart.domain.model.CartItemKey;
import com.groom.e_commerce.cart.domain.repository.CartRepository;
import com.groom.e_commerce.cart.infrastructure.redis.support.CartRedisKeyGenerator;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CartRedisRepositoryImpl implements CartRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CartRedisKeyGenerator keyGenerator;

    @Override
    public void addItem(UUID userId, UUID productId, UUID variantId, int quantity) {
        String cartKey = keyGenerator.cartKey(userId);
        String field = keyGenerator.itemField(productId, variantId);

        redisTemplate.opsForHash().increment(cartKey, field, quantity);
        extendTtl(cartKey);
    }

    @Override
    public Optional<CartItem> findItem(UUID userId, UUID productId, UUID variantId) {
        Object value = redisTemplate.opsForHash().get(
            keyGenerator.cartKey(userId),
            keyGenerator.itemField(productId, variantId)
        );

        if (!(value instanceof Number)) {
            return Optional.empty();
        }

        return Optional.of(
            new CartItem(productId, variantId, ((Number) value).intValue())
        );
    }

    @Override
    public List<CartItem> findAll(UUID userId) {
        Map<Object, Object> entries =
            redisTemplate.opsForHash().entries(keyGenerator.cartKey(userId));

        return entries.entrySet().stream()
            .map(this::toCartItem)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }

    @Override
    public void updateQuantity(UUID userId, UUID productId, UUID variantId, int quantity) {
        String cartKey = keyGenerator.cartKey(userId);
        String field = keyGenerator.itemField(productId, variantId);

        if (quantity <= 0) {
            removeItem(userId, productId, variantId);
            return;
        }

        redisTemplate.opsForHash().put(cartKey, field, quantity);
        extendTtl(cartKey);
    }

    @Override
    public void removeItem(UUID userId, UUID productId, UUID variantId) {
        redisTemplate.opsForHash().delete(
            keyGenerator.cartKey(userId),
            keyGenerator.itemField(productId, variantId)
        );
    }

    @Override
    public void removeItems(UUID userId, List<CartItemKey> keys) {
        if (keys == null || keys.isEmpty()) return;

        Object[] fields = keys.stream()
            .map(k -> keyGenerator.itemField(k.productId(), k.variantId()))
            .toArray();

        redisTemplate.opsForHash().delete(keyGenerator.cartKey(userId), fields);
    }

    @Override
    public void clear(UUID userId) {
        redisTemplate.delete(keyGenerator.cartKey(userId));
    }

    /* ===== private ===== */

    private void extendTtl(String cartKey) {
        redisTemplate.expire(cartKey, keyGenerator.ttlDays(), TimeUnit.DAYS);
    }

    private Optional<CartItem> toCartItem(Map.Entry<Object, Object> entry) {
        if (!(entry.getKey() instanceof String field) ||
            !(entry.getValue() instanceof Number qty)) {
            return Optional.empty();
        }

        String[] parts = field.split(":");
        if (parts.length != 2) return Optional.empty();

        try {
            return Optional.of(
                new CartItem(
                    UUID.fromString(parts[0]),
                    UUID.fromString(parts[1]),
                    qty.intValue()
                )
            );
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
