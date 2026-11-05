package com.groupvm.catalog.application;

import com.groupvm.catalog.domain.Product;
import com.groupvm.catalog.domain.ProductAttributes;
import com.groupvm.catalog.domain.exception.DuplicateSkuException;
import com.groupvm.catalog.domain.exception.ProductValidationException;
import com.groupvm.catalog.application.port.out.ProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateProductServiceValidationTest {

    private static final ProductAttributes ATTRIBUTES = new ProductAttributes(
            100,
            new BigDecimal("1"),
            new BigDecimal("1"),
            new BigDecimal("1")
    );

    @Test
    void rejectsInvalidCreateWithoutSaving() {
        var savedProducts = new ArrayList<Product>();
        var service = service(new InMemoryProductRepository(savedProducts));

        assertThrows(ProductValidationException.class, () -> service.create(
                new CreateProductCommand(" ", "Name", new BigDecimal("10.00"), ATTRIBUTES, "actor")
        ));
        assertThrows(ProductValidationException.class, () -> service.create(
                new CreateProductCommand("SKU", " ", new BigDecimal("10.00"), ATTRIBUTES, "actor")
        ));
        assertThrows(ProductValidationException.class, () -> service.create(
                new CreateProductCommand("SKU", "Name", new BigDecimal("10.001"), ATTRIBUTES, "actor")
        ));
        assertThrows(ProductValidationException.class, () -> service.create(
                new CreateProductCommand("SKU", "Name", BigDecimal.ZERO, ATTRIBUTES, "actor")
        ));
        assertThrows(ProductValidationException.class, () -> service.create(
                new CreateProductCommand("SKU", "Name", null, ATTRIBUTES, "actor")
        ));
        assertThrows(ProductValidationException.class, () -> service.create(
                new CreateProductCommand("SKU", "Name", new BigDecimal("10.00"), ATTRIBUTES, " ")
        ));
        assertEquals(0, savedProducts.size());
    }

    @Test
    void rejectsSkuAlreadyHeldByAnotherProductWithoutSaving() {
        var savedProducts = new ArrayList<Product>();
        savedProducts.add(Product.draft(
                UUID.randomUUID(),
                "SKU-001",
                "Existing product",
                new BigDecimal("10.00"),
                ATTRIBUTES,
                Instant.EPOCH,
                "actor"
        ));
        var service = service(new InMemoryProductRepository(savedProducts));

        assertThrows(DuplicateSkuException.class, () -> service.create(
                new CreateProductCommand(" sku-001 ", "Name", new BigDecimal("10.00"), ATTRIBUTES, "actor")
        ));

        assertEquals(1, savedProducts.size());
    }

    private static CreateProductService service(ProductRepository repository) {
        return new CreateProductService(
                repository,
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                UUID::randomUUID
        );
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
