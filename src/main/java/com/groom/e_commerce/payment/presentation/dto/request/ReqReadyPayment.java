package com.groom.e_commerce.payment.presentation.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ReqReadyPayment(
	@NotNull UUID orderId,
	String orderName,
	String customerName
) {}
