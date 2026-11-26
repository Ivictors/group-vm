package com.groupvm.catalog.application.command;
import java.math.BigDecimal;
public record UpdateProductCommand(String name, String description, boolean descriptionPresent,
                                   BigDecimal price, Integer weightGrams, BigDecimal lengthCm,
                                   BigDecimal widthCm, BigDecimal heightCm) {}
