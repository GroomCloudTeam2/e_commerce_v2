package org.example.cart.application.event;

import java.util.UUID;

public record OrderConfirmedEvent(
    UUID userId,
    UUID orderId
) {}
