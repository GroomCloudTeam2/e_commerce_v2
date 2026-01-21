package com.groom.e_commerce.user.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groom.e_commerce.user.application.service.AddressServiceV1;
import com.groom.e_commerce.user.presentation.dto.request.address.ReqAddressDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.address.ResAddressDtoV1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "User", description = "배송지 API")
@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class AddressControllerV1 {

	private final AddressServiceV1 addressService;

	@Operation(summary = "배송지 목록 조회")
	@GetMapping
	public ResponseEntity<List<ResAddressDtoV1>> getAddresses(
		@RequestHeader("X-User-Id") UUID userId) {
		return ResponseEntity.ok(addressService.getAddresses(userId));
	}

	@Operation(summary = "배송지 등록")
	@PostMapping
	public ResponseEntity<Void> createAddress(
		@RequestHeader("X-User-Id") UUID userId,
		@Valid @RequestBody ReqAddressDtoV1 request) {
		addressService.createAddress(userId, request);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "배송지 수정")
	@PutMapping("/{addressId}")
	public ResponseEntity<Void> updateAddress(
		@RequestHeader("X-User-Id") UUID userId,
		@PathVariable UUID addressId,
		@Valid @RequestBody ReqAddressDtoV1 request) {
		addressService.updateAddress(userId, addressId, request);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "배송지 삭제")
	@DeleteMapping("/{addressId}")
	public ResponseEntity<Void> deleteAddress(
		@RequestHeader("X-User-Id") UUID userId,
		@PathVariable UUID addressId) {
		addressService.deleteAddress(userId, addressId);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "기본 배송지 설정")
	@PostMapping("/{addressId}/set-default")
	public ResponseEntity<Void> setDefaultAddress(
		@RequestHeader("X-User-Id") UUID userId,
		@PathVariable UUID addressId) {
		addressService.setDefaultAddress(userId, addressId);
		return ResponseEntity.ok().build();
	}
}
