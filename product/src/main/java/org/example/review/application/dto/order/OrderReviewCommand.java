package org.example.review.application.dto.order;

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
