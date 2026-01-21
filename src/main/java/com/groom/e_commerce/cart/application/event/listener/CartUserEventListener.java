package com.groom.e_commerce.cart.application.event.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.groom.e_commerce.cart.application.CartService;
import com.groom.e_commerce.cart.application.event.UserDeletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * User 서비스 이벤트 리스너
 * Cart 기준 정책:
 * - 유저 정보 수정: 무시
 * - 유저 탈퇴: 장바구니 전체 삭제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CartUserEventListener {

	private final CartService cartService;

	/**
	 * 유저 탈퇴 시 장바구니 정리
	 */
	@Async("cartEventExecutor")
	@EventListener
	public void handle(UserDeletedEvent event) {
		log.info("유저 탈퇴 이벤트 수신 - userId={}", event.userId());
		cartService.clearCart(event.userId());
	}
}
