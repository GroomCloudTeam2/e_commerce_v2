package com.groom.e_commerce.review.application.dto.order;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderReviewCommand {

	private UUID orderId;
	private UUID productId;
	private UUID userId;
}
