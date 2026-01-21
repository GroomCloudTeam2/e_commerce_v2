package com.groom.e_commerce.review.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.groom.e_commerce.review.application.service.ReviewQueryService;
import com.groom.e_commerce.review.presentation.dto.response.ProductReviewResponse;

@ExtendWith(MockitoExtension.class)
class ProductReviewControllerTest {

	private static final UUID PRODUCT_ID = UUID.randomUUID();

	private MockMvc mockMvc;

	@Mock
	private ReviewQueryService reviewQueryService;

	@BeforeEach
	void setUp() {
		ProductReviewController controller =
			new ProductReviewController(reviewQueryService);

		mockMvc = MockMvcBuilders
			.standaloneSetup(controller)
			.build();
	}

	@Test
	void 상품_리뷰_목록_조회_기본값() throws Exception {
		// given
		when(reviewQueryService.getProductReviews(
			eq(PRODUCT_ID), eq(0), eq(10)
		)).thenReturn(mock(ProductReviewResponse.class));

		// when & then
		mockMvc.perform(get("/api/v1/reviews/product/{productId}/", PRODUCT_ID))
			.andExpect(status().isOk());

		verify(reviewQueryService).getProductReviews(
			PRODUCT_ID, 0, 10
		);
	}

	@Test
	void 상품_리뷰_목록_조회_쿼리파라미터_지정() throws Exception {
		// given
		when(reviewQueryService.getProductReviews(
			eq(PRODUCT_ID), eq(2), eq(5)
		)).thenReturn(mock(ProductReviewResponse.class));

		// when & then
		mockMvc.perform(
				get("/api/v1/reviews/product/{productId}/", PRODUCT_ID)
					.param("page", "2")
					.param("size", "5")
			)
			.andExpect(status().isOk());

		verify(reviewQueryService).getProductReviews(
			PRODUCT_ID, 2, 5
		);
	}
}
