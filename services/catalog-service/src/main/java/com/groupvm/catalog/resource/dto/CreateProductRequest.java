package com.groupvm.catalog.resource.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.groupvm.catalog.application.command.CreateProductCommand;

import java.math.BigDecimal;

public record CreateProductRequest(String sku, String name, String description, BigDecimal price,
                                   Integer weightGrams, BigDecimal lengthCm, BigDecimal widthCm,
                                   BigDecimal heightCm) {
    public static CreateProductRequest from(JsonNode body) {
        ProductJsonInput.object(body, "sku", "name", "description", "price", "weightGrams",
                "lengthCm", "widthCm", "heightCm");
        return new CreateProductRequest(
                ProductJsonInput.text(body, "sku", true),
                ProductJsonInput.text(body, "name", true),
                ProductJsonInput.text(body, "description", false),
                ProductJsonInput.price(body, true),
                ProductJsonInput.weight(body, true),
                ProductJsonInput.dimension(body, "lengthCm", true),
                ProductJsonInput.dimension(body, "widthCm", true),
                ProductJsonInput.dimension(body, "heightCm", true));
    }

    public CreateProductCommand toCommand() {
        return new CreateProductCommand(sku, name, description, price, weightGrams,
                lengthCm, widthCm, heightCm);
    }
}
