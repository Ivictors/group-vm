package com.groupvm.catalog.resource.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.groupvm.catalog.application.command.UpdateProductCommand;

import java.math.BigDecimal;

public record UpdateProductRequest(String name, String description, boolean descriptionPresent,
                                   BigDecimal price, Integer weightGrams, BigDecimal lengthCm,
                                   BigDecimal widthCm, BigDecimal heightCm) {
    public static UpdateProductRequest from(JsonNode body) {
        ProductJsonInput.object(body, "name", "description", "price", "weightGrams",
                "lengthCm", "widthCm", "heightCm");
        if (body.isEmpty()) throw new InvalidProductRequestException("At least one field is required");
        return new UpdateProductRequest(
                ProductJsonInput.text(body, "name", false),
                ProductJsonInput.text(body, "description", false),
                body.has("description"),
                ProductJsonInput.price(body, false),
                ProductJsonInput.weight(body, false),
                ProductJsonInput.dimension(body, "lengthCm", false),
                ProductJsonInput.dimension(body, "widthCm", false),
                ProductJsonInput.dimension(body, "heightCm", false));
    }

    public UpdateProductCommand toCommand() {
        return new UpdateProductCommand(name, description, descriptionPresent, price,
                weightGrams, lengthCm, widthCm, heightCm);
    }
}
