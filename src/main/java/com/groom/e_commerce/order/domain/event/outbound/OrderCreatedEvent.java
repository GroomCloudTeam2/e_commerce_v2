package com.groom.e_commerce.order.domain.event.outbound;

import java.util.UUID;

/**
 * 주문 생성 시 발행되는 이벤트
 */
public record OrderCreatedEvent(UUID orderId, Long amount) {
}