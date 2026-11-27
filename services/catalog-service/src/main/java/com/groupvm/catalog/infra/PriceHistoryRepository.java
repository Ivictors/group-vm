package com.groupvm.catalog.infra;

import com.groupvm.catalog.domain.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {
    List<PriceHistory> findByProductIdOrderByChangedAtAscIdAsc(UUID productId);
}
