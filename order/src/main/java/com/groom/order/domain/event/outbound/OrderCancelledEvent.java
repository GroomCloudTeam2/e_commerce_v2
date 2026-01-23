package com.groom.order.domain.event.outbound;

import java.util.UUID;

/**
 * 주문 취소 시 발행되는 이벤트
 */
public record OrderCancelledEvent(UUID orderId, String reason) {
    public static OrderCancelledEvent of(UUID orderId, String reason) {
        return new OrderCancelledEvent(orderId, reason);
    }
}
