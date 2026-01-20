package com.groom.e_commerce.cart.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.groom.e_commerce.cart.application.CartService;
import com.groom.e_commerce.cart.presentation.controller.CartController;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CartService cartService;

    @Test
    void 장바구니_담기_API() throws Exception {
        mockMvc.perform(
                post("/api/v1/cart")
                    .header("X-USER-ID", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "productId": "00000000-0000-0000-0000-000000000001",
                          "variantId": "00000000-0000-0000-0000-000000000002",
                          "quantity": 1
                        }
                    """)
            )
            .andExpect(status().isCreated());
    }
}
