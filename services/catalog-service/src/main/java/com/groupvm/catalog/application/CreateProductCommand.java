package com.groupvm.catalog.application;

import com.groupvm.catalog.domain.ProductAttributes;

import java.math.BigDecimal;

public record CreateProductCommand(
        String sku,
        String name,
        BigDecimal initialPrice,
        ProductAttributes attributes,
        String actor
) {
}
