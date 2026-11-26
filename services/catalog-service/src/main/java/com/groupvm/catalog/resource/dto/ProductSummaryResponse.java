package com.groupvm.catalog.resource.dto;

import com.groupvm.catalog.domain.Product;
import com.groupvm.catalog.domain.ProductStatus;

import java.time.Instant;
import java.util.UUID;

public record ProductSummaryResponse(UUID id, String sku, String name, ProductResponse.Price price,
                                     ProductStatus status, Instant createdAt, Instant updatedAt) {
    public static ProductSummaryResponse from(Product product) {
        return new ProductSummaryResponse(product.getId(), product.getSku(), product.getName(),
                new ProductResponse.Price("BRL", product.getPrice().toPlainString()),
                product.getStatus(), product.getCreatedAt(), product.getUpdatedAt());
    }
}
