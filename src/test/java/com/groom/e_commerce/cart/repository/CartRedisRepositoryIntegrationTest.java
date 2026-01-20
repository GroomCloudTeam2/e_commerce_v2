package com.groom.e_commerce.cart.repository;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.groom.e_commerce.cart.domain.dto.CartItem;
import com.groom.e_commerce.cart.domain.repository.CartRepository;

@SpringBootTest
@Testcontainers
@Tag("integration")
class CartRedisRepositoryIntegrationTest {

    @Container
    static GenericContainer<?> redis =
        new GenericContainer<>("redis:7.2")
            .withExposedPorts(6379);


    @Autowired
    CartRepository cartRepository;

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Test
    void 레디스에_장바구니_저장_조회() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        cartRepository.addItem(userId, productId, variantId, 2);

        CartItem item = cartRepository
            .findItem(userId, productId, variantId)
            .orElseThrow();

        assertThat(item.getQuantity()).isEqualTo(2);
    }
}
