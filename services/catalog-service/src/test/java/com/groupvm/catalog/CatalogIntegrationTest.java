package com.groupvm.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class CatalogIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired DataSource dataSource;

    @Test
    void persistsAndArchivesProductThroughHttp() throws Exception {
        var sku = "SKU-" + UUID.randomUUID();
        var create = "{\"sku\":\"" + sku + "\",\"name\":\"Chair\",\"description\":\"Wood chair\",\"price\":99.90}";
        var created = mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(create))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sku", is(sku))).andReturn();
        var id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/products/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Chair")));
        var update = "{\"name\":\"Office chair\",\"description\":\"Updated\",\"price\":129.90}";
        mockMvc.perform(put("/api/v1/products/{id}", id).contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name", is("Office chair")));
        mockMvc.perform(patch("/api/v1/products/{id}/archive", id)).andExpect(status().isNoContent());
        mockMvc.perform(put("/api/v1/products/{id}", id).contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isConflict());
    }

    @Test
    void enforcesSkuUniquenessAndNotFoundResponse() throws Exception {
        var sku = "SKU-" + UUID.randomUUID();
        var create = "{\"sku\":\"" + sku + "\",\"name\":\"Table\",\"price\":199.90}";
        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(create))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(create))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.title", is("Product conflict")));
        mockMvc.perform(get("/api/v1/products/{id}", "00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.title", is("Product not found")));
    }

    @Test
    void rejectsInvalidCreatePayload() throws Exception {
        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"\",\"name\":\"\",\"price\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upgradesExistingProductFromV1ToV2WithoutDataLoss() {
        var schema = "catalog_migration_" + UUID.randomUUID().toString().replace("-", "");
        var productId = UUID.randomUUID();
        try {
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                    .defaultSchema(schema).schemas(schema).target("1").load().migrate();
            jdbcTemplate.update("INSERT INTO " + schema + ".products "
                    + "(id, sku, name, price, archived, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, false, now(), now())",
                    productId, "EXISTING-SKU", "Existing product", new BigDecimal("12.50"));

            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                    .defaultSchema(schema).schemas(schema).load().migrate();

            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + schema + ".products WHERE id = ?", Integer.class, productId));
            assertEquals("EXISTING-SKU", jdbcTemplate.queryForObject(
                    "SELECT sku FROM " + schema + ".products WHERE id = ?", String.class, productId));
            assertEquals(new BigDecimal("12.50"), jdbcTemplate.queryForObject(
                    "SELECT price FROM " + schema + ".products WHERE id = ?", BigDecimal.class, productId));
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + schema + ".flyway_schema_history WHERE version = '2' AND success",
                    Integer.class));
            assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                    "INSERT INTO " + schema + ".products "
                            + "(id, sku, name, price, archived, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, false, now(), now())",
                    UUID.randomUUID(), "   ", "Invalid product", new BigDecimal("12.50")));
        } finally {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Test
    void failedUpgradeKeepsV1AndExistingData() {
        var schema = "catalog_migration_" + UUID.randomUUID().toString().replace("-", "");
        var productId = UUID.randomUUID();
        try {
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                    .defaultSchema(schema).schemas(schema).target("1").load().migrate();
            jdbcTemplate.update("INSERT INTO " + schema + ".products "
                            + "(id, sku, name, price, archived, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, false, now(), now())",
                    productId, "   ", "Legacy invalid SKU", new BigDecimal("12.50"));

            var flyway = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                    .defaultSchema(schema).schemas(schema).load();
            assertThrows(FlywayException.class, flyway::migrate);

            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + schema + ".products WHERE id = ?", Integer.class, productId));
            assertEquals(0, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + schema + ".flyway_schema_history WHERE version = '2'",
                    Integer.class));
        } finally {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }
}
