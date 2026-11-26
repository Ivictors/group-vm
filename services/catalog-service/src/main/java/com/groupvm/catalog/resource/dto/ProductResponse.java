package com.groupvm.catalog.resource.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.groupvm.catalog.domain.Product;
import com.groupvm.catalog.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductResponse(UUID id, String sku, String name, String description, PriceResponse price,
                              Integer weightGrams, BigDecimal lengthCm, BigDecimal widthCm,
                              BigDecimal heightCm, ProductStatus status, Instant createdAt,
                              Instant updatedAt, List<?> priceHistory) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(),
                product.getDescription(), PriceResponse.from(product),
                product.getWeightGrams(), product.getLengthCm(), product.getWidthCm(),
                product.getHeightCm(), product.getStatus(), product.getCreatedAt(),
                product.getUpdatedAt(), List.of());
    }
}
