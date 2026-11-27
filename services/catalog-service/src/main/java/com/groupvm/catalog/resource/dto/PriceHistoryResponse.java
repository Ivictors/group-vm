package com.groupvm.catalog.resource.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.groupvm.catalog.domain.PriceHistory;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PriceHistoryResponse(Instant occurredAt, String actor, String oldAmount, String newAmount) {
    public static PriceHistoryResponse from(PriceHistory entry) {
        return new PriceHistoryResponse(entry.getChangedAt(), entry.getActor(),
                entry.getOldAmount() == null ? null : entry.getOldAmount().toPlainString(),
                entry.getAmount().toPlainString());
    }
}
