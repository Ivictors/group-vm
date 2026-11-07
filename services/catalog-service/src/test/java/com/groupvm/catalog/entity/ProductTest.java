package com.groupvm.catalog.entity;

import com.groupvm.catalog.exception.ProductValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTest {

    private static final ProductAttributes ATTRIBUTES = new ProductAttributes(
            100,
            new BigDecimal("1"),
            new BigDecimal("1"),
            new BigDecimal("1")
    );

    @Test
    void rejectsMissingRequiredDraftValues() {
        assertThrows(ProductValidationException.class, () -> Product.draft(
                null, "SKU", "Name", new BigDecimal("10.00"), ATTRIBUTES,
                Instant.EPOCH, "actor"
        ));
        assertThrows(ProductValidationException.class, () -> Product.draft(
                UUID.randomUUID(), " ", "Name", new BigDecimal("10.00"), ATTRIBUTES,
                Instant.EPOCH, "actor"
        ));
        assertThrows(ProductValidationException.class, () -> Product.draft(
                UUID.randomUUID(), "SKU", "Name", new BigDecimal("10.00"), null,
                Instant.EPOCH, "actor"
        ));
        assertThrows(ProductValidationException.class, () -> Product.draft(
                UUID.randomUUID(), "SKU", "Name", new BigDecimal("10.00"), ATTRIBUTES,
                Instant.EPOCH, " "
        ));
    }

    @Test
    void trimsValuesWhenCreatedDirectlyByTheDomain() {
        Product product = Product.draft(
                UUID.randomUUID(), " SKU-001 ", " Example product ", new BigDecimal("10.00"),
                ATTRIBUTES, Instant.EPOCH, "actor"
        );

        assertEquals("SKU-001", product.sku());
        assertEquals("Example product", product.name());
    }
}
