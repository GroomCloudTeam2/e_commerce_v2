package com.groom.e_commerce.order.application.handler;

import com.groom.e_commerce.order.application.service.OrderService;
import com.groom.e_commerce.order.domain.event.inbound.StockDeductedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 관련 이벤트를 처리하는 핸들러.
 */
@Component
@RequiredArgsConstructor
public class StockEventHandler {

    private final OrderService orderService;

    @Transactional
    public void handleStockDeducted(StockDeductedEvent event) {
        orderService.confirmOrder(event.getOrderId());
    }
}
