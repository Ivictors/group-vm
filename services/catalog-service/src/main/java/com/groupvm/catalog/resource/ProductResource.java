package com.groupvm.catalog.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.groupvm.catalog.application.ProductService;
import com.groupvm.catalog.resource.dto.CreateProductRequest;
import com.groupvm.catalog.resource.dto.ProductPageResponse;
import com.groupvm.catalog.resource.dto.ProductResponse;
import com.groupvm.catalog.resource.dto.UpdateProductRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@Profile({"local", "test", "integration"})
@RequestMapping("/api/v1/admin/products")
public class ProductResource {
    private final ProductService service;

    public ProductResource(ProductService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody JsonNode body) {
        var product = service.create(CreateProductRequest.from(body).toCommand());
        return ResponseEntity.created(URI.create("/api/v1/admin/products/" + product.getId()))
                .body(ProductResponse.from(product));
    }

    @GetMapping
    public ProductPageResponse list(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return ProductPageResponse.from(service.list(page, size));
    }

    @GetMapping("/{productId}")
    public ProductResponse get(@PathVariable UUID productId) {
        return ProductResponse.from(service.get(productId));
    }

    @PatchMapping("/{productId}")
    public ProductResponse update(@PathVariable UUID productId, @RequestBody JsonNode body) {
        return ProductResponse.from(service.update(productId, UpdateProductRequest.from(body).toCommand()));
    }

    @PostMapping("/{productId}/activate")
    public ProductResponse activate(@PathVariable UUID productId) {
        return ProductResponse.from(service.activate(productId));
    }

    @PostMapping("/{productId}/archive")
    public ProductResponse archive(@PathVariable UUID productId) {
        return ProductResponse.from(service.archive(productId));
    }
}
