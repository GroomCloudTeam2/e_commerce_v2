package com.groom.e_commerce.cart.application.event;

import java.util.UUID;

public record CartOrderConfirmedEvent(
    UUID userId,
    UUID orderId
) {}
