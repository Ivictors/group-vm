package com.groupvm.catalog.resource.dto;

import com.groupvm.catalog.domain.Product;

public record PriceResponse(String currency, String amount) {
    public static PriceResponse from(Product product) {
        return new PriceResponse("BRL", product.getPrice().toPlainString());
    }
}
