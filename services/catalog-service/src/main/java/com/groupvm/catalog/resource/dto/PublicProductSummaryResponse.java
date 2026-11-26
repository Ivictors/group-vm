package com.groupvm.catalog.resource.dto;

import com.groupvm.catalog.domain.Product;

import java.util.UUID;

public record PublicProductSummaryResponse(UUID id, String name, PriceResponse price) {
    public static PublicProductSummaryResponse from(Product product) {
        return new PublicProductSummaryResponse(product.getId(), product.getName(),
                PriceResponse.from(product));
    }
}
