package com.groom.e_commerce.order.domain.event.inbound;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StockDeductionFailedEvent {
    private UUID orderId;
    private String reason;
}
