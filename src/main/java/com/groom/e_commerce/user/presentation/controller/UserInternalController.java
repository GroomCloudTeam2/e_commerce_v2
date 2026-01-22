package com.groom.e_commerce.user.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.user.domain.entity.address.AddressEntity;
import com.groom.e_commerce.user.domain.entity.user.UserEntity;
import com.groom.e_commerce.user.domain.repository.AddressRepository;
import com.groom.e_commerce.user.domain.repository.UserRepository;
import com.groom.e_commerce.user.presentation.dto.response.internal.UserAddressInternalResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "User Internal", description = "사용자 내부 API (서비스 간 통신용)")
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserInternalController {

	private final UserRepository userRepository;
	private final AddressRepository addressRepository;

	@Operation(summary = "사용자 유효성 검증", description = "사용자 존재 여부 및 탈퇴 여부를 검증합니다.")
	@GetMapping("/{userId}/validate")
	public ResponseEntity<Void> validateUser(@PathVariable UUID userId) {
		log.info("[Internal API] 사용자 유효성 검증 요청 - userId: {}", userId);

		UserEntity user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		if (user.isWithdrawn()) {
			throw new CustomException(ErrorCode.ALREADY_WITHDRAWN);
		}

		log.info("[Internal API] 사용자 유효성 검증 완료 - userId: {}", userId);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "사용자 배송지 조회", description = "주문 스냅샷용 기본 배송지 정보를 조회합니다.")
	@GetMapping("/{userId}/address")
	public ResponseEntity<UserAddressInternalResponse> getUserAddress(@PathVariable UUID userId) {
		log.info("[Internal API] 사용자 배송지 조회 요청 - userId: {}", userId);

		AddressEntity address = addressRepository.findByUserUserIdAndIsDefaultTrue(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.ADDRESS_NOT_FOUND));

		UserAddressInternalResponse response = UserAddressInternalResponse.builder()
			.recipientName(address.getRecipient())
			.recipientPhone(address.getRecipientPhone())
			.zipCode(address.getZipCode())
			.address(address.getAddress())
			.detailAddress(address.getDetailAddress())
			.build();

		log.info("[Internal API] 사용자 배송지 조회 완료 - userId: {}", userId);
		return ResponseEntity.ok(response);
	}
}