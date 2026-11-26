package com.groupvm.catalog.resource;

import com.groupvm.catalog.application.PublicProductService;
import com.groupvm.catalog.resource.dto.PublicProductPageResponse;
import com.groupvm.catalog.resource.dto.PublicProductResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class PublicProductResource {
    private final PublicProductService service;

    public PublicProductResource(PublicProductService service) {
        this.service = service;
    }

    @GetMapping
    public PublicProductPageResponse list(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return PublicProductPageResponse.from(service.list(page, size));
    }

    @GetMapping("/{productId}")
    public PublicProductResponse get(@PathVariable UUID productId) {
        return PublicProductResponse.from(service.get(productId));
    }
}
