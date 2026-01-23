package org.example.review.infrastructure.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderReviewValidationResponse {

	private boolean orderExists;
	private boolean ownerMatched;
	private boolean containsProduct;
	private String orderStatus;
	private boolean reviewable;

	public static OrderReviewValidationResponse fail() {
		return new OrderReviewValidationResponse(
			false,
			false,
			false,
			null,
			false
		);
	}
}
