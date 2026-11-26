package com.groupvm.catalog.resource.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.groupvm.catalog.domain.Product;

import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicProductResponse(UUID id, String name, String description, PriceResponse price,
                                    Integer weightGrams, BigDecimal lengthCm, BigDecimal widthCm,
                                    BigDecimal heightCm) {
    public static PublicProductResponse from(Product product) {
        return new PublicProductResponse(product.getId(), product.getName(), product.getDescription(),
                PriceResponse.from(product),
                product.getWeightGrams(), product.getLengthCm(), product.getWidthCm(), product.getHeightCm());
    }
}
