package com.groupvm.catalog.application;

import com.groupvm.catalog.application.command.CreateProductCommand;
import com.groupvm.catalog.application.command.UpdateProductCommand;
import com.groupvm.catalog.application.exception.DuplicateSkuException;
import com.groupvm.catalog.application.exception.ProductNotFoundException;
import com.groupvm.catalog.domain.Product;
import com.groupvm.catalog.infra.ProductRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public Product create(CreateProductCommand command) {
        if (repository.existsBySku(command.sku())) throw new DuplicateSkuException(command.sku());
        return repository.save(new Product(command.sku(), command.name(), command.description(), command.price()));
    }

    public List<Product> list() {
        return repository.findAll();
    }

    public Product get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Product update(UUID id, UpdateProductCommand command) {
        Product p = get(id);
        p.update(command.name(), command.description(), command.price());
        return repository.save(p);
    }

    public void archive(UUID id) {
        Product p = get(id);
        p.archive();
        repository.save(p);
    }

}
