package com.groom.e_commerce.order.application.handler;

import com.groom.e_commerce.order.application.service.OrderService;
import com.groom.e_commerce.order.domain.event.inbound.RefundFailedEvent;
import com.groom.e_commerce.order.domain.event.inbound.RefundSucceededEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 환불 관련 이벤트를 처리하는 핸들러.
 */
@Component
@RequiredArgsConstructor
public class RefundEventHandler {

    private final OrderService orderService;

    @Transactional
    public void handleRefundSuccess(RefundSucceededEvent event) {
        orderService.cancelOrder(event.getOrderId());
    }

    @Transactional
    public void handleRefundFailure(RefundFailedEvent event) {
        orderService.failRefund(event.getOrderId());
    }
}
