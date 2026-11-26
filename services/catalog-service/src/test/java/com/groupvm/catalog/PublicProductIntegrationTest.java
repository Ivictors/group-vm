package com.groupvm.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupvm.catalog.domain.Product;
import com.groupvm.catalog.infra.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class PublicProductIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ProductRepository repository;
    @Autowired JdbcTemplate jdbc;

    @Test
    void filtersInPostgresqlPaginatesMixedStatusesAndHidesNonActiveIds() throws Exception {
        List<UUID> ids = new ArrayList<>();
        long previousActiveCount = repository.findByStatus(com.groupvm.catalog.domain.ProductStatus.ACTIVE,
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
        try {
            Product oldest = save(ids, "Oldest", true, false);
            Product middle = save(ids, "Middle", true, false);
            Product newest = save(ids, "Newest", true, false);
            Product draft = save(ids, "Draft", false, false);
            Product archived = save(ids, "Archived", true, true);

            Instant baseline = Instant.parse("2100-01-01T00:00:00Z");
            for (int i = 0; i < ids.size(); i++) {
                jdbc.update("UPDATE products SET created_at = ? WHERE id = ?",
                        Timestamp.from(baseline.plusSeconds(i)), ids.get(i));
            }

            mvc.perform(get("/api/v1/products").param("page", "0").param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].id", is(newest.getId().toString())))
                    .andExpect(jsonPath("$.items[1].id", is(middle.getId().toString())))
                    .andExpect(jsonPath("$.totalElements", is((int) previousActiveCount + 3)))
                    .andExpect(jsonPath("$.totalPages", is((int) ((previousActiveCount + 4) / 2))))
                    .andExpect(jsonPath("$.items[0].sku").doesNotExist());
            mvc.perform(get("/api/v1/products").param("page", "1").param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].id", is(oldest.getId().toString())));
            mvc.perform(get("/api/v1/products/{id}", newest.getId()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.weightGrams", is(4500)))
                    .andExpect(jsonPath("$.sku").doesNotExist())
                    .andExpect(jsonPath("$.status").doesNotExist());

            String unknownBody = mvc.perform(get("/api/v1/products/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
            for (UUID nonActive : List.of(draft.getId(), archived.getId())) {
                String hiddenBody = mvc.perform(get("/api/v1/products/{id}", nonActive))
                        .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
                assertEquals(objectMapper.readTree(unknownBody).get("title"), objectMapper.readTree(hiddenBody).get("title"));
                assertEquals(objectMapper.readTree(unknownBody).get("detail"), objectMapper.readTree(hiddenBody).get("detail"));
            }
            assertEquals(previousActiveCount + 3, repository.findByStatus(
                    com.groupvm.catalog.domain.ProductStatus.ACTIVE,
                    org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements());
        } finally {
            for (UUID id : ids) jdbc.update("DELETE FROM products WHERE id = ?", id);
        }
    }

    private Product save(List<UUID> ids, String name, boolean active, boolean archive) {
        Product product = new Product("PUBLIC-" + UUID.randomUUID(), name, "Wooden chair",
                new BigDecimal("129.90"), 4500, BigDecimal.valueOf(55),
                BigDecimal.valueOf(50), BigDecimal.valueOf(90));
        if (active) product.activate();
        if (archive) product.archive();
        repository.saveAndFlush(product);
        ids.add(product.getId());
        return product;
    }
}
