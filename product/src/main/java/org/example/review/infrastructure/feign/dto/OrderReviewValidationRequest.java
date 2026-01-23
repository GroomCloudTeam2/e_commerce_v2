package org.example.review.infrastructure.feign.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderReviewValidationRequest {

	private UUID orderId;
	private UUID productId;
	private UUID userId;
}
