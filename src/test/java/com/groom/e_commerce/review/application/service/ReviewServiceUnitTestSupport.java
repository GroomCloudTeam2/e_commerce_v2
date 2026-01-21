package com.groom.e_commerce.review.application.service;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groom.e_commerce.global.infrastructure.client.Classification.AiRestClient;
import com.groom.e_commerce.review.application.validator.OrderReviewValidator;
import com.groom.e_commerce.review.domain.repository.ProductRatingRepository;
import com.groom.e_commerce.review.domain.repository.ReviewLikeRepository;
import com.groom.e_commerce.review.domain.repository.ReviewRepository;

@ExtendWith(MockitoExtension.class)
abstract class ReviewServiceUnitTestSupport {

	@InjectMocks
	protected ReviewService reviewService;

	@Mock
	protected ReviewRepository reviewRepository;
	@Mock
	protected ReviewLikeRepository reviewLikeRepository;
	@Mock
	protected ProductRatingRepository productRatingRepository;
	@Mock
	protected AiRestClient aiRestClient;
	@Mock
	protected OrderReviewValidator orderReviewValidator;

	protected UUID userId;
	protected UUID productId;
	protected UUID orderId;
	protected UUID reviewId;

	@BeforeEach
	void setUp() {
		userId = UUID.randomUUID();
		productId = UUID.randomUUID();
		orderId = UUID.randomUUID();
		reviewId = UUID.randomUUID();
	}
}
