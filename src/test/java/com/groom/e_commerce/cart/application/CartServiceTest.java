package com.groom.e_commerce.cart.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groom.e_commerce.cart.application.dto.ProductCartInfo;
import com.groom.e_commerce.cart.domain.repository.CartRepository;
import com.groom.e_commerce.cart.infrastructure.feign.ProductClient;
import com.groom.e_commerce.cart.presentation.dto.request.CartAddRequest;
import com.groom.e_commerce.global.presentation.advice.CustomException;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    CartRepository cartRepository;

    @Mock
    ProductClient productClient;

    @InjectMocks
    CartService cartService;

    @Test
    void 장바구니_상품_추가_성공() {
        // given
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        CartAddRequest request = new CartAddRequest(productId, variantId, 2);

        ProductCartInfo productInfo = mock(ProductCartInfo.class);
        given(productInfo.isAvailable()).willReturn(true);
        given(productInfo.getStockQuantity()).willReturn(10);

        given(productClient.getProductCartInfos(any()))
            .willReturn(List.of(productInfo));

        given(cartRepository.findItem(userId, productId, variantId))
            .willReturn(Optional.empty());

        // when
        cartService.addItemToCart(userId, request);

        // then
        verify(cartRepository).addItem(userId, productId, variantId, 2);
    }

    @Test
    void 재고_부족하면_예외() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        CartAddRequest request = new CartAddRequest(productId, variantId, 5);

        ProductCartInfo productInfo = mock(ProductCartInfo.class);
        given(productInfo.isAvailable()).willReturn(true);
        given(productInfo.getStockQuantity()).willReturn(3);

        given(productClient.getProductCartInfos(any()))
            .willReturn(List.of(productInfo));

        assertThrows(CustomException.class,
            () -> cartService.addItemToCart(userId, request));
    }
}
