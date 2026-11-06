package com.groupvm.catalog.service;

import com.groupvm.catalog.entity.Product;
import com.groupvm.catalog.exception.DuplicateSkuException;
import com.groupvm.catalog.exception.ProductValidationException;
import com.groupvm.catalog.repository.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

public final class CreateProductService {
    private final ProductRepository repository;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;

    public CreateProductService(ProductRepository repository, Clock clock, Supplier<UUID> idGenerator) {
        this.repository = repository;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    public Product create(CreateProductCommand command) {
        String sku = trimRequired(command.sku(), "SKU");
        String name = trimRequired(command.name(), "Name");
        String actor = trimRequired(command.actor(), "Actor");
        BigDecimal price = normalizePrice(command.initialPrice());

        if (repository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateSkuException(sku);
        }

        Product product = Product.draft(
                idGenerator.get(),
                sku,
                name,
                price,
                command.attributes(),
                clock.instant(),
                actor
        );
        return repository.save(product);
    }

    private static String trimRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ProductValidationException(field + " must not be blank");
        }
        return value.trim();
    }

    private static BigDecimal normalizePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0 || price.scale() > 2) {
            throw new ProductValidationException("Price must be positive with at most two decimal places");
        }
        return price.setScale(2, RoundingMode.UNNECESSARY);
    }
}
