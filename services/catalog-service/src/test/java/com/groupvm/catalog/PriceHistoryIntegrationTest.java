package com.groupvm.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupvm.catalog.application.ProductService;
import com.groupvm.catalog.application.command.UpdateProductCommand;
import com.groupvm.catalog.domain.PriceHistory;
import com.groupvm.catalog.domain.Product;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.dao.DataAccessException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@WithMockUser(username = "operator-1", authorities = "catalog_admin")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PriceHistoryIntegrationTest {
    private static UUID persistedAcrossContexts;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductService service;
    @Autowired TransactionTemplate transactions;
    @Autowired EntityManagerFactory entityManagers;
    @Autowired DataSource dataSource;

    @Test
    @Order(1)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void writesHistoryBeforeContextRestart() throws Exception {
        persistedAcrossContexts = create();
        mvc.perform(patch("/api/v1/admin/products/{id}", persistedAcrossContexts)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"price\":{\"currency\":\"BRL\",\"amount\":\"105.00\"}}"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    void readsSameAuditAfterContextRestart() throws Exception {
        assertNotNull(persistedAcrossContexts);
        mvc.perform(get("/api/v1/admin/products/{id}", persistedAcrossContexts))
                .andExpect(status().isOk()).andExpect(jsonPath("$.priceHistory.length()", is(2)))
                .andExpect(jsonPath("$.priceHistory[0].newAmount", is("99.90")))
                .andExpect(jsonPath("$.priceHistory[1].newAmount", is("105.00")));
    }

    @Test
    void createAndUpdatePersistOrderedHistoryWithoutDuplicateOrForgedActor() throws Exception {
        UUID id = create();
        mvc.perform(patch("/api/v1/admin/products/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"price\":{\"currency\":\"BRL\",\"amount\":\"99.90\"}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.priceHistory.length()", is(1)));
        mvc.perform(patch("/api/v1/admin/products/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"price\":{\"currency\":\"BRL\",\"amount\":\"101.25\"}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.priceHistory.length()", is(2)))
                .andExpect(jsonPath("$.priceHistory[1].oldAmount", is("99.90")))
                .andExpect(jsonPath("$.priceHistory[1].newAmount", is("101.25")));
        mvc.perform(patch("/api/v1/admin/products/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"price\":{\"currency\":\"BRL\",\"amount\":\"102.00\"},\"actor\":\"forged\"}"))
                .andExpect(status().isBadRequest());
        assertEquals(2, count(id));
        assertEquals("operator-1", jdbc.queryForObject(
                "SELECT actor FROM product_price_history WHERE product_id = ? ORDER BY changed_at, id LIMIT 1",
                String.class, id));
        mvc.perform(get("/api/v1/admin/products/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.priceHistory[0].newAmount", is("99.90")))
                .andExpect(jsonPath("$.priceHistory[1].newAmount", is("101.25")));
    }

    @Test
    void outerFailureRollsBackProductAndHistoryTogether() throws Exception {
        UUID id = create();
        int before = count(id);
        assertThrows(IllegalStateException.class, () -> transactions.executeWithoutResult(status -> {
            service.update(id, new UpdateProductCommand(null, null, false, new BigDecimal("110.00"),
                    null, null, null, null), "operator-2");
            throw new IllegalStateException("test rollback");
        }));
        assertEquals(before, count(id));
        assertEquals(new BigDecimal("99.90"), jdbc.queryForObject(
                "SELECT price FROM products WHERE id = ?", BigDecimal.class, id));
    }

    @Test
    void staleWriteCannotCommitOrLeaveOrphanedAudit() throws Exception {
        UUID id = create();
        try (var first = entityManagers.createEntityManager(); var stale = entityManagers.createEntityManager()) {
            first.getTransaction().begin();
            stale.getTransaction().begin();
            Product firstProduct = first.find(Product.class, id);
            Product staleProduct = stale.find(Product.class, id);
            firstProduct.update(null, null, false, new BigDecimal("111.00"), null, null, null, null);
            first.persist(new PriceHistory(id, new BigDecimal("99.90"), new BigDecimal("111.00"), "first"));
            first.getTransaction().commit();
            staleProduct.update(null, null, false, new BigDecimal("112.00"), null, null, null, null);
            stale.persist(new PriceHistory(id, new BigDecimal("99.90"), new BigDecimal("112.00"), "stale"));
            assertThrows(PersistenceException.class, () -> stale.getTransaction().commit());
        }
        assertEquals(2, count(id));
        assertEquals(new BigDecimal("111.00"), jdbc.queryForObject(
                "SELECT price FROM products WHERE id = ?", BigDecimal.class, id));
    }

    @Test
    void migrationFromExistingProductsDoesNotInventHistoricalActorAndIsRepeatable() {
        String schema = "catalog_price_" + UUID.randomUUID().toString().replace("-", "");
        UUID id = UUID.randomUUID();
        try {
            var baseline = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                    .defaultSchema(schema).schemas(schema).target("3").load();
            baseline.migrate();
            jdbc.update("INSERT INTO " + schema + ".products "
                    + "(id, sku, name, price, status, version, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, 'DRAFT', 0, now(), now())",
                    id, "LEGACY-" + id, "Legacy", new BigDecimal("18.50"));
            var upgraded = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                    .defaultSchema(schema).schemas(schema).load();
            upgraded.migrate();
            upgraded.migrate();
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + schema
                    + ".product_price_history WHERE product_id = ?", Integer.class, id));
            assertEquals(new BigDecimal("18.50"), jdbc.queryForObject(
                    "SELECT price FROM " + schema + ".products WHERE id = ?", BigDecimal.class, id));
        } finally {
            jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Test
    void databaseRejectsRewritingOrDeletingAuditEntries() throws Exception {
        UUID id = create();
        UUID entry = jdbc.queryForObject("SELECT id FROM product_price_history WHERE product_id = ?",
                UUID.class, id);
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "UPDATE product_price_history SET amount = 1.00 WHERE id = ?", entry));
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "DELETE FROM product_price_history WHERE id = ?", entry));
        assertEquals(1, count(id));
    }

    private UUID create() throws Exception {
        var response = mvc.perform(post("/api/v1/admin/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"SKU-" + UUID.randomUUID()
                        + "\",\"name\":\"Chair\",\"price\":{\"currency\":\"BRL\",\"amount\":\"99.90\"},"
                        + "\"weightGrams\":100,\"lengthCm\":1,\"widthCm\":1,\"heightCm\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priceHistory[0].actor", is("operator-1"))).andReturn();
        return UUID.fromString(mapper.readTree(response.getResponse().getContentAsString()).get("id").asText());
    }

    private int count(UUID id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM product_price_history WHERE product_id = ?",
                Integer.class, id);
    }
}
