package com.groom.e_commerce.review.application.dto.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderReviewResult {

	private boolean orderExists;
	private boolean ownerMatched;
	private boolean containsProduct;
	private boolean reviewable;
}
