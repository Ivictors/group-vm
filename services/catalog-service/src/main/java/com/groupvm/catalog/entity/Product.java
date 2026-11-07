package com.groupvm.catalog.entity;

import com.groupvm.catalog.exception.ProductValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Product {
    private final UUID id;
    private final String sku;
    private final String name;
    private final ProductAttributes attributes;
    private final ProductStatus status;
    private final List<PriceRecord> priceHistory;

    private Product(
            UUID id,
            String sku,
            String name,
            ProductAttributes attributes,
            ProductStatus status,
            List<PriceRecord> priceHistory
    ) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.attributes = attributes;
        this.status = status;
        this.priceHistory = List.copyOf(priceHistory);
    }

    public static Product draft(
            UUID id,
            String sku,
            String name,
            BigDecimal initialPrice,
            ProductAttributes attributes,
            Instant createdAt,
            String actor
    ) {
        UUID requiredId = requireId(id);
        String normalizedSku = requiredText(sku, "SKU");
        String normalizedName = requiredText(name, "Name");
        if (attributes == null) {
            throw new ProductValidationException("Attributes must not be null");
        }
        return new Product(
                requiredId,
                normalizedSku,
                normalizedName,
                attributes,
                ProductStatus.DRAFT,
                List.of(new PriceRecord(createdAt, actor, null, initialPrice))
        );
    }

    private static UUID requireId(UUID id) {
        if (id == null) {
            throw new ProductValidationException("Product ID must not be null");
        }
        return id;
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ProductValidationException(field + " must not be blank");
        }
        return value.trim();
    }

    public UUID id() {
        return id;
    }

    public String sku() {
        return sku;
    }

    public String name() {
        return name;
    }

    public ProductAttributes attributes() {
        return attributes;
    }

    public ProductStatus status() {
        return status;
    }

    public List<PriceRecord> priceHistory() {
        return priceHistory;
    }

    public BigDecimal currentPrice() {
        return priceHistory.getLast().newAmount();
    }
}
