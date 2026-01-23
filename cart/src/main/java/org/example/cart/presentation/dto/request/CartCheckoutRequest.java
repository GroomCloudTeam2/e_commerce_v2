package org.example.cart.presentation.dto.request;

import java.util.List;

import com.groom.e_commerce.cart.domain.model.CartItemKey;

import lombok.Getter;

@Getter
public class CartCheckoutRequest {
    private List<CartItemKey> selectedItems;
}
