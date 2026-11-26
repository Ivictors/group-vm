package com.groupvm.catalog.domain;

import com.groupvm.catalog.domain.exception.InvalidProductException;
import com.groupvm.catalog.domain.exception.InvalidTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {
    private static final BigDecimal PRICE = new BigDecimal("10.00");

    private Product draft() {
        return new Product(" sku-1 ", " Chair ", null, PRICE, 100,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
    }

    @Test
    void creationNormalizesTextAndStartsAsDraft() {
        Product product = draft();
        assertNotNull(product.getId());
        assertEquals("sku-1", product.getSku());
        assertEquals("Chair", product.getName());
        assertEquals(ProductStatus.DRAFT, product.getStatus());
        assertEquals(100, product.getWeightGrams());
    }

    @Test
    void creationRejectsInvalidRequiredFields() {
        assertThrows(InvalidProductException.class, () -> new Product(" ", "Chair", null, PRICE, 100, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
        assertThrows(InvalidProductException.class, () -> new Product("SKU", "  ", null, PRICE, 100, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
        assertThrows(InvalidProductException.class, () -> new Product("SKU", "Chair", null, new BigDecimal("0.00"), 100, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
        assertThrows(InvalidProductException.class, () -> new Product("SKU", "Chair", null, new BigDecimal("10.001"), 100, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
        assertThrows(InvalidProductException.class, () -> new Product("SKU", "Chair", null, new BigDecimal("100000000000000000.00"), 100, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
        assertThrows(InvalidProductException.class, () -> new Product("SKU", "Chair", null, PRICE, 0, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
        assertThrows(InvalidProductException.class, () -> new Product("SKU", "Chair", null, PRICE, 100, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE));
        assertThrows(InvalidProductException.class, () -> new Product("SKU", "Chair", null, PRICE, 100, BigDecimal.ONE, null, BigDecimal.ONE));
    }

    @Test
    void creationRejectsUnsupportedControlWhitespaceAnywhereInSku() {
        for (int codePoint = 28; codePoint <= 31; codePoint++) {
            String sku = "SKU" + (char) codePoint + "-1";
            InvalidProductException exception = assertThrows(InvalidProductException.class,
                    () -> new Product(sku, "Chair", null, PRICE, 100,
                            BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
            assertTrue(exception.getMessage().contains("sku"));
        }
    }

    @Test
    void rejectsNullCharacterInPersistedTextFields() {
        assertThrows(InvalidProductException.class, () -> new Product("SK\u0000U", "Chair", null, PRICE,
                100, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
        assertThrows(InvalidProductException.class, () -> new Product("SKU", "Cha\u0000ir", null, PRICE,
                100, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
        assertThrows(InvalidProductException.class, () -> new Product("SKU", "Chair", "De\u0000tail", PRICE,
                100, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));

        Product product = draft();
        assertThrows(InvalidProductException.class,
                () -> product.update("Cha\u0000ir", null, false, null, null, null, null, null));
        assertThrows(InvalidProductException.class,
                () -> product.update(null, "De\u0000tail", true, null, null, null, null, null));
        assertEquals("Chair", product.getName());
        assertNull(product.getDescription());
    }

    @Test
    void draftCanBeActivatedThenArchivedButArchivedIsTerminal() {
        Product product = draft();
        product.activate();
        assertEquals(ProductStatus.ACTIVE, product.getStatus());
        assertThrows(InvalidTransitionException.class, product::activate);
        product.archive();
        assertEquals(ProductStatus.ARCHIVED, product.getStatus());
        assertThrows(InvalidTransitionException.class, product::archive);
        assertThrows(InvalidTransitionException.class, product::activate);
        assertThrows(InvalidTransitionException.class, () -> product.update("New", null, false, null, null, null, null, null));
    }

    @Test
    void draftCanBeArchivedDirectly() {
        Product product = draft();
        product.archive();
        assertEquals(ProductStatus.ARCHIVED, product.getStatus());
    }

    @Test
    void partialUpdateKeepsOmittedFieldsAndAllowsDescriptionClear() {
        Product product = draft();
        product.update(" New chair ", "Ignored", false, null, null, null, null, null);
        assertEquals("New chair", product.getName());
        assertEquals(PRICE, product.getPrice());
        product.update(null, "Detail", true, null, null, null, null, null);
        assertEquals("Detail", product.getDescription());
        product.update(null, null, true, null, null, null, null, null);
        assertNull(product.getDescription());
        assertEquals("sku-1", product.getSku());
    }

    @Test
    void invalidUpdateDoesNotPartiallyModifyProduct() {
        Product product = draft();
        assertThrows(InvalidProductException.class, () -> product.update("Valid", null, false, new BigDecimal("1.234"), null, null, null, null));
        assertEquals("Chair", product.getName());
    }
}
