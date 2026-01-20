package com.groom.e_commerce.cart.application.event.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.groom.e_commerce.cart.application.CartService;
import com.groom.e_commerce.cart.application.event.OrderConfirmedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartOrderEventListener {

    private final CartService cartService;

    /**
     * 주문 최종 확정 후 장바구니 비우기
     */
    @Async("cartEventExecutor")
    @EventListener
    public void handle(OrderConfirmedEvent event) {
        log.info("주문 확정 이벤트 수신 - userId={}, orderId={}",
            event.userId(), event.orderId());

        cartService.clearCart(event.userId());
    }
}
