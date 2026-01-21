package com.groom.e_commerce.order.infrastructure.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.groom.e_commerce.order.infrastructure.client.dto.UserAddressResponse;

@FeignClient(
	name = "user-service",
	path = "/internal/users"
)
public interface UserClient {

	/**
	 * 사용자 유효성 검증
	 * - 존재 여부
	 * - 탈퇴/정지 여부
	 */
	@GetMapping("/{userId}/validate")
	static void isValidUser(
		@PathVariable("userId") UUID userId,
		@RequestHeader("X-User-Id") UUID currentUserId // 필요 시 헤더 전달
	) {

	}

	/**
	 * 배송지 정보 조회 (스냅샷 생성용)
	 */
	@GetMapping("/{userId}/address")
	UserAddressResponse getUserAddress(
		@PathVariable("userId") UUID userId,
		@RequestHeader("X-User-Id") UUID currentUserId // 필요 시 헤더 전달
	);
}
