package com.groupvm.catalog.domain;

import com.groupvm.catalog.domain.exception.InvalidProductException;
import com.groupvm.catalog.domain.exception.InvalidTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {
    @Id
    private UUID id;
    @Column(nullable = false, length = 80, updatable = false)
    private String sku;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(length = 2000)
    private String description;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;
    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    private Integer weightGrams;
    @Column(columnDefinition = "numeric")
    private BigDecimal lengthCm;
    @Column(columnDefinition = "numeric")
    private BigDecimal widthCm;
    @Column(columnDefinition = "numeric")
    private BigDecimal heightCm;
    @Version
    private Long version;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected Product() {
    }

    public Product(String sku, String name, String description, BigDecimal price,
                   Integer weightGrams, BigDecimal lengthCm, BigDecimal widthCm, BigDecimal heightCm) {
        this.sku = requiredText(sku, 80, "sku");
        this.name = requiredText(name, 160, "name");
        this.description = description(description);
        this.price = price(price);
        this.weightGrams = weight(weightGrams);
        this.lengthCm = dimension(lengthCm, "lengthCm");
        this.widthCm = dimension(widthCm, "widthCm");
        this.heightCm = dimension(heightCm, "heightCm");
        this.id = UUID.randomUUID();
        this.status = ProductStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void update(String name, String description, boolean descriptionPresent,
                       BigDecimal price, Integer weightGrams, BigDecimal lengthCm,
                       BigDecimal widthCm, BigDecimal heightCm) {
        if (status == ProductStatus.ARCHIVED) throw new InvalidTransitionException("Archived products cannot be updated");
        if (name == null && !descriptionPresent && price == null && weightGrams == null
                && lengthCm == null && widthCm == null && heightCm == null) {
            throw new InvalidProductException("At least one field is required");
        }
        String nextName = name == null ? this.name : requiredText(name, 160, "name");
        String nextDescription = descriptionPresent ? description(description) : this.description;
        BigDecimal nextPrice = price == null ? this.price : price(price);
        Integer nextWeight = weightGrams == null ? this.weightGrams : weight(weightGrams);
        BigDecimal nextLength = lengthCm == null ? this.lengthCm : dimension(lengthCm, "lengthCm");
        BigDecimal nextWidth = widthCm == null ? this.widthCm : dimension(widthCm, "widthCm");
        BigDecimal nextHeight = heightCm == null ? this.heightCm : dimension(heightCm, "heightCm");
        this.name = nextName;
        this.description = nextDescription;
        this.price = nextPrice;
        this.weightGrams = nextWeight;
        this.lengthCm = nextLength;
        this.widthCm = nextWidth;
        this.heightCm = nextHeight;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != ProductStatus.DRAFT || weightGrams == null || lengthCm == null
                || widthCm == null || heightCm == null) {
            throw new InvalidTransitionException("Only complete draft products can be activated");
        }
        status = ProductStatus.ACTIVE;
        updatedAt = Instant.now();
    }

    public void archive() {
        if (status == ProductStatus.ARCHIVED) throw new InvalidTransitionException("Product is already archived");
        status = ProductStatus.ARCHIVED;
        updatedAt = Instant.now();
    }

    private static String requiredText(String value, int maxLength, String field) {
        if (value == null || value.strip().isEmpty()) throw new InvalidProductException(field + " is required");
        if (value.indexOf('\0') >= 0 || ("sku".equals(field) && value.chars().anyMatch(ch -> ch >= 28 && ch <= 31))) {
            throw new InvalidProductException(field + " contains invalid characters");
        }
        String trimmed = value.strip();
        if (trimmed.length() > maxLength) throw new InvalidProductException(field + " is too long");
        return trimmed;
    }

    private static String description(String value) {
        if (value != null && value.indexOf('\0') >= 0) throw new InvalidProductException("description contains invalid characters");
        if (value != null && value.length() > 2000) throw new InvalidProductException("description is too long");
        return value;
    }

    private static BigDecimal price(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.scale() != 2 || value.precision() - value.scale() > 17) {
            throw new InvalidProductException("price must be positive BRL with exactly two decimal places");
        }
        return value;
    }

    private static Integer weight(Integer value) {
        if (value == null || value <= 0) throw new InvalidProductException("weightGrams must be positive");
        return value;
    }

    private static BigDecimal dimension(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) throw new InvalidProductException(field + " must be positive");
        return value;
    }

    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public ProductStatus getStatus() { return status; }
    public Integer getWeightGrams() { return weightGrams; }
    public BigDecimal getLengthCm() { return lengthCm; }
    public BigDecimal getWidthCm() { return widthCm; }
    public BigDecimal getHeightCm() { return heightCm; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
