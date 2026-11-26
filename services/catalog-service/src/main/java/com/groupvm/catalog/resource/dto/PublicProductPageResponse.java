package com.groupvm.catalog.resource.dto;

import com.groupvm.catalog.domain.Product;
import org.springframework.data.domain.Page;

import java.util.List;

public record PublicProductPageResponse(List<PublicProductSummaryResponse> items, int page, int size,
                                        long totalElements, int totalPages) {
    public static PublicProductPageResponse from(Page<Product> products) {
        return new PublicProductPageResponse(products.getContent().stream()
                .map(PublicProductSummaryResponse::from).toList(), products.getNumber(), products.getSize(),
                products.getTotalElements(), products.getTotalPages());
    }
}
