package com.groom.e_commerce.review.application.event;

import java.util.UUID;

public record ReviewCreatedEvent(
	UUID userId,
	UUID reviewId,
	UUID productId,
	int rating
) {}
