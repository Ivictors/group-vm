package com.groupvm.catalog.domain;

import com.groupvm.catalog.domain.exception.ProductValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PriceRecordTest {

    @Test
    void rejectsMissingOrInvalidPriceRecordValues() {
        assertThrows(ProductValidationException.class, () -> new PriceRecord(
                null, "actor", null, new BigDecimal("10.00")
        ));
        assertThrows(ProductValidationException.class, () -> new PriceRecord(
                Instant.EPOCH, " ", null, new BigDecimal("10.00")
        ));
        assertThrows(ProductValidationException.class, () -> new PriceRecord(
                Instant.EPOCH, "actor", null, new BigDecimal("10.001")
        ));
        assertThrows(ProductValidationException.class, () -> new PriceRecord(
                Instant.EPOCH, "actor", null, BigDecimal.ZERO
        ));
    }

    @Test
    void normalizesAmountsToTwoDecimalPlaces() {
        PriceRecord record = new PriceRecord(
                Instant.EPOCH, "actor", new BigDecimal("8.5"), new BigDecimal("10.9")
        );

        assertEquals(new BigDecimal("8.50"), record.oldAmount());
        assertEquals(new BigDecimal("10.90"), record.newAmount());
    }
}
