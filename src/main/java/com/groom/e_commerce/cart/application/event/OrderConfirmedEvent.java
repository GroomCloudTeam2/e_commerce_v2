package com.groom.e_commerce.cart.application.event;

import java.util.UUID;

public record OrderConfirmedEvent(
    UUID userId,
    UUID orderId
) {}
