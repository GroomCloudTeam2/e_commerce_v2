package com.groom.e_commerce.order.application.event;

import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.event.outbound.OrderConfirmedEvent;
import com.groom.e_commerce.order.domain.repository.OrderRepository;
import com.groom.e_commerce.payment.event.model.PaymentCompletedEvent;
// import com.groom.e_commerce.payment.event.model.PaymentFailedEvent;
// import com.groom.e_commerce.payment.event.model.RefundFailedEvent;
import com.groom.e_commerce.payment.event.model.RefundSucceededEvent;
import com.groom.e_commerce.product.application.event.dto.StockDeductedEvent;
import com.groom.e_commerce.product.application.event.dto.StockDeductionFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Payment completed for order: {}", event.orderId());
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + event.orderId()));

        order.confirmPayment();
        orderRepository.save(order);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStockDeducted(StockDeductedEvent event) {
        log.info("Stock deducted for order: {}", event.getOrderId());
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + event.getOrderId()));

        order.complete();
        orderRepository.save(order);

        eventPublisher.publishEvent(new OrderConfirmedEvent(order.getUserId(), order.getOrderId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Payment failed for order: {}", event.orderId());
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + event.orderId()));

        order.fail();
        orderRepository.save(order);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStockDeductionFailed(StockDeductionFailedEvent event) {
        log.info("Stock deduction failed for order: {}", event.getOrderId());
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + event.getOrderId()));

        order.fail();
        orderRepository.save(order);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleRefundSucceeded(RefundSucceededEvent event) {
        log.info("Refund succeeded for order: {}", event.orderId());
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + event.orderId()));

        order.cancel();
        orderRepository.save(order);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleRefundFailed(RefundFailedEvent event) {
        log.error("Refund failed for order: {}", event.orderId());
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + event.orderId()));

        order.requireManualCheck();
        orderRepository.save(order);
    }
}
