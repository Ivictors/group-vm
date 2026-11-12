package com.groupvm.catalog.resource.dto;
import com.groupvm.catalog.domain.Product; import java.math.BigDecimal; import java.util.UUID;
public record ProductResponse(UUID id,String sku,String name,String description,BigDecimal price,boolean archived) { public static ProductResponse from(Product p) { return new ProductResponse(p.getId(),p.getSku(),p.getName(),p.getDescription(),p.getPrice(),p.isArchived()); } }
