package com.groom.e_commerce.review.application.event;

import java.util.UUID;

public record ReviewCreatedEvent(
	UUID reviewId,
	UUID productId,
	int rating
) {}
