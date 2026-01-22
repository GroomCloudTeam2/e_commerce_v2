package com.groom.e_commerce.order.infrastructure.client.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StockReserveRequest {

	private UUID productId;
	private UUID variantId;
	private int quantity;
}
