package com.groupvm.catalog.domain;

import com.groupvm.catalog.domain.exception.ProductValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record PriceRecord(
        Instant changedAt,
        String actor,
        BigDecimal oldAmount,
        BigDecimal newAmount
) {
    public PriceRecord {
        if (changedAt == null) {
            throw new ProductValidationException("Price change time must not be null");
        }
        actor = requiredText(actor, "Actor");
        oldAmount = normalizeAmount(oldAmount, "Old amount", true);
        newAmount = normalizeAmount(newAmount, "New amount", false);
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ProductValidationException(field + " must not be blank");
        }
        return value.trim();
    }

    private static BigDecimal normalizeAmount(BigDecimal amount, String field, boolean nullable) {
        if (amount == null && nullable) {
            return null;
        }
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new ProductValidationException(field + " must be positive with at most two decimal places");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }
}
