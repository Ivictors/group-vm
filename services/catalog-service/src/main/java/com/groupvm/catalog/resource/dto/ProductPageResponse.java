package com.groupvm.catalog.resource.dto;

import com.groupvm.catalog.domain.Product;
import org.springframework.data.domain.Page;

import java.util.List;

public record ProductPageResponse(List<ProductSummaryResponse> items, int page, int size,
                                  long totalElements, int totalPages) {
    public static ProductPageResponse from(Page<Product> products) {
        return new ProductPageResponse(products.getContent().stream().map(ProductSummaryResponse::from).toList(),
                products.getNumber(), products.getSize(), products.getTotalElements(), products.getTotalPages());
    }
}
