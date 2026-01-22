package com.groom.e_commerce.payment.presentation.dto.response;

import java.util.UUID;

public record ResReadyPayment(
	String pg,           // "TOSS"
	String clientKey,
	UUID orderId,
	Long amount,
	String orderName,
	String customerName,
	String successUrl,
	String failUrl
) {}
