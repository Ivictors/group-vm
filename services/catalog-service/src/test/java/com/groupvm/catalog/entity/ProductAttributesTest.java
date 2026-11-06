package com.groupvm.catalog.entity;

import com.groupvm.catalog.exception.ProductValidationException;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductAttributesTest {

    @Test
    void rejectsNonPositiveWeight() {
        assertThrows(ProductValidationException.class, () -> validAttributes(0));
    }

    @Test
    void rejectsNonPositiveDimensions() {
        assertThrows(ProductValidationException.class, () -> new ProductAttributes(
                100,
                BigDecimal.ZERO,
                new BigDecimal("1"),
                new BigDecimal("1")
        ));
    }

    private static ProductAttributes validAttributes(int weight) {
        return new ProductAttributes(
                weight,
                new BigDecimal("1"),
                new BigDecimal("1"),
                new BigDecimal("1")
        );
    }
}
