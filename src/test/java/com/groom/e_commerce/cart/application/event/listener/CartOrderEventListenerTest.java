package com.groom.e_commerce.cart.application.event;

@ExtendWith(MockitoExtension.class)
class CartOrderEventListenerTest {

	@InjectMocks
	private CartOrderEventListener listener;

	@Mock
	private CartService cartService;

	@Test
	void handle_shouldClearCart_whenOrderConfirmedEventReceived() {
		// given
		UUID userId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();

		OrderConfirmedEvent event = new OrderConfirmedEvent(userId, orderId);

		// when
		listener.handle(event);

		// then
		verify(cartService).clearCart(userId);
	}
}
