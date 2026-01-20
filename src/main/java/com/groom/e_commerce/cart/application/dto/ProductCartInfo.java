package com.groom.e_commerce.cart.application.dto;

import java.util.UUID;

import lombok.Getter;

@Getter
public class ProductCartInfo {

    private UUID productId;
    private UUID variantId;

    private String productName;
    private String optionName;
    private String thumbnailUrl;

    private int price;
    private int stockQuantity;
    private boolean available;
}
