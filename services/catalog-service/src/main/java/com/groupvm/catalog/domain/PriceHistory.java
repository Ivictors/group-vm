package com.groupvm.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_price_history")
public class PriceHistory {
    @Id
    private UUID id;
    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;
    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;
    @Column(name = "old_amount", precision = 19, scale = 2, updatable = false)
    private BigDecimal oldAmount;
    @Column(nullable = false, columnDefinition = "text", updatable = false)
    private String actor;
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected PriceHistory() { }

    public PriceHistory(UUID productId, BigDecimal oldAmount, BigDecimal amount, String actor) {
        if (productId == null || !validPrice(amount) || (oldAmount != null && !validPrice(oldAmount))
                || actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("Valid product, price and actor are required");
        }
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.amount = amount;
        this.oldAmount = oldAmount;
        this.actor = actor;
        this.changedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getOldAmount() { return oldAmount; }
    public String getActor() { return actor; }
    public Instant getChangedAt() { return changedAt; }

    private static boolean validPrice(BigDecimal value) {
        return value != null && value.signum() > 0 && value.scale() == 2
                && value.precision() - value.scale() <= 17;
    }
}
