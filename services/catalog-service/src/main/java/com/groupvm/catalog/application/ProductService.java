package com.groupvm.catalog.application;

import com.groupvm.catalog.application.command.CreateProductCommand;
import com.groupvm.catalog.application.command.UpdateProductCommand;
import com.groupvm.catalog.application.exception.DuplicateSkuException;
import com.groupvm.catalog.application.exception.ProductNotFoundException;
import com.groupvm.catalog.domain.Product;
import com.groupvm.catalog.domain.PriceHistory;
import com.groupvm.catalog.infra.PriceHistoryRepository;
import com.groupvm.catalog.infra.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

@Service
public class ProductService {
    private final ProductRepository repository;
    private final PriceHistoryRepository history;

    public ProductService(ProductRepository repository, PriceHistoryRepository history) {
        this.repository = repository;
        this.history = history;
    }

    @Transactional
    public Product create(CreateProductCommand command, String actor) {
        validateActor(actor);
        Product product = new Product(command.sku(), command.name(), command.description(), command.price(),
                command.weightGrams(), command.lengthCm(), command.widthCm(), command.heightCm());
        if (repository.existsBySkuIgnoreCase(product.getSku())) throw new DuplicateSkuException(product.getSku());
        Product saved = repository.save(product);
        history.save(new PriceHistory(saved.getId(), null, saved.getPrice(), actor));
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Product> list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid page or size");
        return repository.findAll(PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    }

    @Transactional(readOnly = true)
    public Product get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public Product update(UUID id, UpdateProductCommand command, String actor) {
        validateActor(actor);
        Product product = find(id);
        var previousPrice = product.getPrice();
        product.update(command.name(), command.description(), command.descriptionPresent(), command.price(),
                command.weightGrams(), command.lengthCm(), command.widthCm(), command.heightCm());
        Product saved = repository.save(product);
        if (previousPrice.compareTo(saved.getPrice()) != 0) {
            history.save(new PriceHistory(saved.getId(), previousPrice, saved.getPrice(), actor));
        }
        return saved;
    }

    @Transactional
    public Product activate(UUID id) {
        Product product = find(id);
        product.activate();
        return repository.save(product);
    }

    @Transactional
    public Product archive(UUID id) {
        Product product = find(id);
        product.archive();
        return repository.save(product);
    }

    private Product find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<PriceHistory> history(UUID id) {
        return history.findByProductIdOrderByChangedAtAscIdAsc(id);
    }

    private static void validateActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("Valid actor is required");
        }
    }
}
