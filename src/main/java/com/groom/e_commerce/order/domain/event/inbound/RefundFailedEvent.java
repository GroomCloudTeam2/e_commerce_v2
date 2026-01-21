package com.groom.e_commerce.order.domain.event.inbound;

import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RefundFailedEvent {
	private final UUID orderId;
}
