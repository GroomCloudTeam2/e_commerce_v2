package org.example.cart.presentation.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CartCheckoutResponse {
    private UUID orderId;
}
