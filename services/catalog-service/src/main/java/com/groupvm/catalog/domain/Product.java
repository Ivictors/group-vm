package com.groupvm.catalog.domain;

import com.groupvm.catalog.domain.exception.ArchivedProductException;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(name = "uk_products_sku", columnNames = "sku"))
public class Product {
    @Id
    private UUID id;
    @Column(nullable = false, length = 80)
    private String sku;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(length = 2000)
    private String description;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;
    @Column(nullable = false)
    private boolean archived;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected Product() {
    }

    public Product(String sku, String name, String description, BigDecimal price) {
        this.id = UUID.randomUUID();
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void update(String name, String description, BigDecimal price) {
        if (archived) throw new ArchivedProductException();
        this.name = name;
        this.description = description;
        this.price = price;
        this.updatedAt = Instant.now();
    }

    public void archive() {
        archived = true;
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isArchived() {
        return archived;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
