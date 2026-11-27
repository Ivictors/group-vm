package com.groupvm.catalog.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PriceHistoryTest {
    @Test
    void rejectsPricesThatWouldBeRoundedOrViolateProductRules() {
        UUID id = UUID.randomUUID();
        for (String invalid : new String[] {"0.00", "-1.00", "1.0", "1.001", "100000000000000000.00"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new PriceHistory(id, null, new BigDecimal(invalid), "operator"));
            assertThrows(IllegalArgumentException.class,
                    () -> new PriceHistory(id, new BigDecimal(invalid), new BigDecimal("10.00"), "operator"));
        }
    }

    @Test
    void preservesExactAmountsAndActor() {
        var history = new PriceHistory(UUID.randomUUID(), new BigDecimal("10.00"),
                new BigDecimal("11.25"), "operator-1");
        assertEquals(new BigDecimal("10.00"), history.getOldAmount());
        assertEquals(new BigDecimal("11.25"), history.getAmount());
        assertEquals("operator-1", history.getActor());
        assertNotNull(history.getChangedAt());
        assertEquals("subject-" + "a".repeat(300),
                new PriceHistory(UUID.randomUUID(), null, new BigDecimal("10.00"),
                        "subject-" + "a".repeat(300)).getActor());
    }
}
