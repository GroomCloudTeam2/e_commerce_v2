package org.example.payment.presentation.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReqConfirmPayment(
	@NotBlank String paymentKey,
	@NotNull UUID orderId,
	@NotNull @Min(1) Long amount
) {}
