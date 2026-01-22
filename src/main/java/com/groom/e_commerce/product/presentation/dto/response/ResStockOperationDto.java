package com.groom.e_commerce.product.presentation.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Internal API - 재고 작업 결과 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ResStockOperationDto {

	private boolean success;
	private String message;

	public static ResStockOperationDto success() {
		return ResStockOperationDto.builder()
			.success(true)
			.message("재고 작업이 완료되었습니다.")
			.build();
	}

	public static ResStockOperationDto success(String message) {
		return ResStockOperationDto.builder()
			.success(true)
			.message(message)
			.build();
	}

	public static ResStockOperationDto fail(String message) {
		return ResStockOperationDto.builder()
			.success(false)
			.message(message)
			.build();
	}
}
