package com.groupvm.catalog.application.command;
import java.math.BigDecimal;
public record UpdateProductCommand(String name, String description, BigDecimal price) {}
