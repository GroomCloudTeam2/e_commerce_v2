package com.groom.e_commerce.cart.application.event;

import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groom.e_commerce.cart.application.CartService;
import com.groom.e_commerce.cart.application.event.listener.CartOrderEventListener;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    CartService cartService;

    @InjectMocks
	CartOrderEventListener listener;

    @Test
    void 주문확정_이벤트_수신시_장바구니_삭제() {
        UUID userId = UUID.randomUUID();
        OrderConfirmedEvent event =
            new OrderConfirmedEvent(userId, UUID.randomUUID());

        listener.handle(event);

        verify(cartService).clearCart(userId);
    }
}
