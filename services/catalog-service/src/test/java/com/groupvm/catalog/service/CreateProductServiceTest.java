package com.groupvm.catalog.service;

import com.groupvm.catalog.entity.Product;
import com.groupvm.catalog.entity.ProductAttributes;
import com.groupvm.catalog.entity.ProductStatus;
import com.groupvm.catalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CreateProductServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-05T12:00:00Z");
    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void createsDraftProductWithTrimmedValuesAndInitialPriceRecord() {
        var savedProducts = new ArrayList<Product>();
        ProductRepository repository = new InMemoryProductRepository(savedProducts);
        var service = new CreateProductService(
                repository,
                Clock.fixed(CREATED_AT, ZoneOffset.UTC),
                () -> PRODUCT_ID
        );

        var command = new CreateProductCommand(
                " SKU-001 ",
                " Example product ",
                new BigDecimal("19.90"),
                new ProductAttributes(250, new BigDecimal("10.0"), new BigDecimal("5.0"), new BigDecimal("2.0")),
                "catalog-admin"
        );

        Product product = service.create(command);

        assertEquals(ProductStatus.DRAFT, product.status());
        assertEquals("SKU-001", product.sku());
        assertEquals("Example product", product.name());
        assertEquals(new BigDecimal("19.90"), product.currentPrice());
        assertEquals(CREATED_AT, product.priceHistory().getFirst().changedAt());
        assertEquals("catalog-admin", product.priceHistory().getFirst().actor());
        assertNull(product.priceHistory().getFirst().oldAmount());
        assertEquals(new BigDecimal("19.90"), product.priceHistory().getFirst().newAmount());
        assertEquals(1, savedProducts.size());
        assertEquals(PRODUCT_ID, product.id());
    }

    private static final class InMemoryProductRepository implements ProductRepository {
        private final List<Product> products;

        private InMemoryProductRepository(List<Product> products) {
            this.products = products;
        }

        @Override
        public boolean existsBySkuIgnoreCase(String sku) {
            return products.stream().anyMatch(product -> product.sku().equalsIgnoreCase(sku));
        }

        @Override
        public Product save(Product product) {
            products.add(product);
            return product;
        }
    }
}
