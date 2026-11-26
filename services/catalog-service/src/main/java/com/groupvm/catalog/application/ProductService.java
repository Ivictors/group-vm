package com.groupvm.catalog.application;

import com.groupvm.catalog.application.command.CreateProductCommand;
import com.groupvm.catalog.application.command.UpdateProductCommand;
import com.groupvm.catalog.application.exception.DuplicateSkuException;
import com.groupvm.catalog.application.exception.ProductNotFoundException;
import com.groupvm.catalog.domain.Product;
import com.groupvm.catalog.infra.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Product create(CreateProductCommand command) {
        Product product = new Product(command.sku(), command.name(), command.description(), command.price(),
                command.weightGrams(), command.lengthCm(), command.widthCm(), command.heightCm());
        if (repository.existsBySkuIgnoreCase(product.getSku())) throw new DuplicateSkuException(product.getSku());
        return repository.save(product);
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
    public Product update(UUID id, UpdateProductCommand command) {
        Product product = find(id);
        product.update(command.name(), command.description(), command.descriptionPresent(), command.price(),
                command.weightGrams(), command.lengthCm(), command.widthCm(), command.heightCm());
        return repository.save(product);
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
}
