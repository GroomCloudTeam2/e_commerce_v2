package com.groom.e_commerce.cart.application.event.request;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CartCheckoutRequestedEvent {

    private UUID userId;
    private UUID orderId;
    private List<CartOrderItem> items;

    @Getter
    @AllArgsConstructor
    public static class CartOrderItem {
        private UUID productId;
        private UUID variantId;
        private int quantity;
    }
}
