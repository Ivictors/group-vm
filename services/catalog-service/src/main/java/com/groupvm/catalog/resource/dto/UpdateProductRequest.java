package com.groupvm.catalog.resource.dto;
import jakarta.validation.constraints.*; import java.math.BigDecimal;
public record UpdateProductRequest(@NotBlank @Size(max=160) String name,@Size(max=2000) String description,@NotNull @DecimalMin("0.01") BigDecimal price) {}
