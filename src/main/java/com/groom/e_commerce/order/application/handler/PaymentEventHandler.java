package com.groom.e_commerce.order.application.handler;

import com.groom.e_commerce.order.application.service.OrderService;
import com.groom.e_commerce.order.domain.event.inbound.PaymentFailEvent;
import com.groom.e_commerce.order.domain.event.inbound.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 관련 이벤트를 처리하는 핸들러.
 */
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final OrderService orderService;

    @Transactional
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        orderService.completePayment(event.getOrderId());
    }

    @Transactional
    public void handlePaymentFailure(PaymentFailEvent event) {
        orderService.failPayment(event.getOrderId());
    }
}
