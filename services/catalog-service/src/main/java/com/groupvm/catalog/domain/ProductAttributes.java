package com.groupvm.catalog.domain;

import com.groupvm.catalog.domain.exception.ProductValidationException;

import java.math.BigDecimal;

public record ProductAttributes(
        int weightGrams,
        BigDecimal lengthCentimeters,
        BigDecimal widthCentimeters,
        BigDecimal heightCentimeters
) {
    public ProductAttributes {
        if (weightGrams <= 0) {
            throw new ProductValidationException("Weight must be positive");
        }
        requirePositive(lengthCentimeters, "Length");
        requirePositive(widthCentimeters, "Width");
        requirePositive(heightCentimeters, "Height");
    }

    private static void requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new ProductValidationException(field + " must be positive");
        }
    }
}
