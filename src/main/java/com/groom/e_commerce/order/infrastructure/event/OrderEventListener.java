package com.groom.e_commerce.order.infrastructure.event;

import com.groom.e_commerce.order.application.handler.PaymentEventHandler;
import com.groom.e_commerce.order.application.handler.RefundEventHandler;
import com.groom.e_commerce.order.application.handler.StockEventHandler;
import com.groom.e_commerce.order.domain.event.inbound.PaymentFailEvent;
import com.groom.e_commerce.order.domain.event.inbound.PaymentSuccessEvent;
import com.groom.e_commerce.order.domain.event.inbound.RefundFailedEvent;
import com.groom.e_commerce.order.domain.event.inbound.RefundSucceededEvent;
import com.groom.e_commerce.order.domain.event.inbound.StockDeductedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 외부 도메인으로부터의 인바운드 이벤트를 수신하는 리스너.
 * 이 리스너는 Application Layer의 Handler에게 이벤트 처리를 위임하는 얇은 계층입니다.
 * 비즈니스 로직을 포함하지 않으며, 트랜잭션과 비동기 처리를 관리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final PaymentEventHandler paymentEventHandler;
    private final StockEventHandler stockEventHandler;
    private final RefundEventHandler refundEventHandler;

    @Async
    @TransactionalEventListener
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        // log.debug("결제 성공 이벤트 수신: {}", orderId);
        paymentEventHandler.handlePaymentSuccess(event);
    }

    @Async
    @TransactionalEventListener
    public void onPaymentFailure(PaymentFailEvent event) {
        paymentEventHandler.handlePaymentFailure(event);
    }

    @Async
    @TransactionalEventListener
    public void onStockDeducted(StockDeductedEvent event) {
        stockEventHandler.handleStockDeducted(event);
    }

    @Async
    @TransactionalEventListener
    public void onRefundSuccess(RefundSucceededEvent event) {
        refundEventHandler.handleRefundSuccess(event);
    }

    @Async
    @TransactionalEventListener
    public void onRefundFailure(RefundFailedEvent event) {
        refundEventHandler.handleRefundFailure(event);
    }
}
