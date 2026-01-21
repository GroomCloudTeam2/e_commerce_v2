package com.groom.e_commerce.cart.infrastructure.feign;

import java.util.List;

import org.springframework.stereotype.Component;

import com.groom.e_commerce.cart.application.dto.ProductCartInfo;
import com.groom.e_commerce.cart.application.dto.StockManagement;
import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;

@Component
public class ProductClientFallback implements ProductClient {

    @Override
    public List<ProductCartInfo> getProductCartInfos(List<StockManagement> requests) {
        throw new CustomException(ErrorCode.PRODUCT_SERVICE_ERROR);
    }
}
