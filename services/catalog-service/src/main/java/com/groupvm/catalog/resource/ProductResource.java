package com.groupvm.catalog.resource;

import com.groupvm.catalog.application.ProductService;
import com.groupvm.catalog.application.command.CreateProductCommand;
import com.groupvm.catalog.application.command.UpdateProductCommand;
import com.groupvm.catalog.resource.dto.CreateProductRequest;
import com.groupvm.catalog.resource.dto.ProductResponse;
import com.groupvm.catalog.resource.dto.UpdateProductRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductResource {
    private final ProductService service;
    public ProductResource(ProductService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        var command = new CreateProductCommand(request.sku(), request.name(), request.description(), request.price());
        return ProductResponse.from(service.create(command));
    }
    @GetMapping public List<ProductResponse> list() { return service.list().stream().map(ProductResponse::from).toList(); }
    @GetMapping("/{id}") public ProductResponse get(@PathVariable UUID id) { return ProductResponse.from(service.get(id)); }
    @PutMapping("/{id}") public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        var command = new UpdateProductCommand(request.name(), request.description(), request.price());
        return ProductResponse.from(service.update(id, command));
    }
    @PatchMapping("/{id}/archive") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) { service.archive(id); }
}
