package com.groupvm.catalog.service;

import com.groupvm.catalog.entity.ProductAttributes;

import java.math.BigDecimal;

public record CreateProductCommand(
        String sku,
        String name,
        BigDecimal initialPrice,
        ProductAttributes attributes,
        String actor
) {
}
