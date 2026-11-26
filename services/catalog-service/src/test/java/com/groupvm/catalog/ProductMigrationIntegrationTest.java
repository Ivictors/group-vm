package com.groupvm.catalog;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductMigrationIntegrationTest {
    private final DriverManagerDataSource dataSource = new DriverManagerDataSource(
            System.getenv("CATALOG_DB_URL"), System.getenv("CATALOG_DB_USERNAME"),
            System.getenv("CATALOG_DB_PASSWORD"));
    private final JdbcTemplate jdbc = new JdbcTemplate(dataSource);

    @Test
    void migratesLegacyRowsWithoutInventingShippingMeasurements() {
        withSchema(schema -> {
            migrate(schema, "2");
            UUID draftId = UUID.randomUUID();
            UUID archivedId = UUID.randomUUID();
            insertLegacy(schema, draftId, " Draft-1 ", false);
            insertLegacy(schema, archivedId, "ARCHIVED-1", true);
            Timestamp createdAt = value(schema, "created_at", draftId, Timestamp.class);
            Timestamp updatedAt = value(schema, "updated_at", draftId, Timestamp.class);

            migrate(schema, null);
            migrate(schema, null);

            assertEquals("DRAFT", value(schema, "status", draftId, String.class));
            assertEquals("ARCHIVED", value(schema, "status", archivedId, String.class));
            assertEquals(draftId, value(schema, "id", draftId, UUID.class));
            assertEquals("Legacy product", value(schema, "name", draftId, String.class));
            assertEquals("Preserved description", value(schema, "description", draftId, String.class));
            assertEquals("Draft-1", value(schema, "sku", draftId, String.class));
            assertEquals(new BigDecimal("12.50"), value(schema, "price", draftId, BigDecimal.class));
            assertEquals(createdAt, value(schema, "created_at", draftId, Timestamp.class));
            assertEquals(updatedAt, value(schema, "updated_at", draftId, Timestamp.class));
            assertEquals(0L, value(schema, "version", draftId, Long.class));
            assertNull(value(schema, "weight_grams", draftId, Integer.class));
            assertNull(value(schema, "length_cm", draftId, BigDecimal.class));
            assertNull(value(schema, "width_cm", draftId, BigDecimal.class));
            assertNull(value(schema, "height_cm", draftId, BigDecimal.class));
            assertNull(value(schema, "weight_grams", archivedId, Integer.class));
            assertNull(value(schema, "length_cm", archivedId, BigDecimal.class));
            assertNull(value(schema, "width_cm", archivedId, BigDecimal.class));
            assertNull(value(schema, "height_cm", archivedId, BigDecimal.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM " + schema
                    + ".flyway_schema_history WHERE version = '3' AND success", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM pg_indexes"
                    + " WHERE schemaname = ? AND indexname = 'uk_products_sku_ci'", Integer.class, schema));
        });
    }

    @Test
    void concurrentInsertsCannotClaimTheSameNormalizedSku() {
        withSchema(schema -> {
            migrate(schema, null);
            String sku = "RACE-" + UUID.randomUUID();
            var started = new CountDownLatch(1);
            var secondPid = new AtomicInteger();
            try (Connection first = dataSource.getConnection();
                 var executor = Executors.newSingleThreadExecutor()) {
                first.setAutoCommit(false);
                int firstPid = backendPid(first);
                insertConcurrent(first, schema, sku);
                var second = executor.submit(() -> {
                    try (Connection other = dataSource.getConnection()) {
                        other.setAutoCommit(false);
                        secondPid.set(backendPid(other));
                        started.countDown();
                        insertConcurrent(other, schema, sku.toLowerCase(java.util.Locale.ROOT));
                        other.commit();
                        return "inserted";
                    } catch (SQLException exception) {
                        return exception.getSQLState();
                    }
                });
                try {
                    org.junit.jupiter.api.Assertions.assertTrue(started.await(5, TimeUnit.SECONDS));
                    assertBlockedBy(secondPid.get(), firstPid);
                    first.commit();
                    assertEquals("23505", second.get(5, TimeUnit.SECONDS));
                } finally {
                    if (!first.getAutoCommit()) first.rollback();
                }
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM " + schema
                        + ".products WHERE lower(sku) = lower(?)", Integer.class, sku));
            } catch (Exception exception) {
                throw new AssertionError("Concurrent SKU check failed", exception);
            }
        });
    }

    @Test
    void createsFreshSchemaWithLifecycleAndCaseInsensitiveSkuConstraint() {
        withSchema(schema -> {
            migrate(schema, null);
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM " + schema
                    + ".flyway_schema_history WHERE version = '3' AND success", Integer.class));
            insertProduct(schema, UUID.randomUUID(), "CHAIR-1", "DRAFT", 1, "1.2345");
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> insertProduct(schema, UUID.randomUUID(), "chair-1", "ARCHIVED", 1, "1.2345"));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> insertProduct(schema, UUID.randomUUID(), "CHAIR-2", "ACTIVE", 0, "1.2345"));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> insertProduct(schema, UUID.randomUUID(), "CHAIR-3", "ACTIVE", 1, "0"));
            var unsupported = assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> insertProduct(schema, UUID.randomUUID(), "\u001cCONTROL-1", "DRAFT", 1, "1"));
            assertTrue(unsupported.getMessage().contains("chk_products_sku_supported_whitespace"));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> jdbc.update("INSERT INTO " + schema + ".products"
                                    + " (id, sku, name, price, status, created_at, updated_at)"
                                    + " VALUES (?, 'PRICE-3', 'Invalid', ?, 'DRAFT', now(), now())",
                            UUID.randomUUID(), new BigDecimal("12.345")));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> jdbc.update("INSERT INTO " + schema + ".products"
                                    + " (id, sku, name, price, status, created_at, updated_at)"
                                    + " VALUES (?, 'INCOMPLETE-ACTIVE', 'Invalid', 12.50, 'ACTIVE', now(), now())",
                            UUID.randomUUID()));
        });
    }

    @Test
    void conflictingLegacySkusAbortUpgradeWithoutChangingLegacyData() {
        withSchema(schema -> {
            migrate(schema, "2");
            UUID first = UUID.randomUUID();
            UUID second = UUID.randomUUID();
            insertLegacy(schema, first, "Chair-1", false);
            insertLegacy(schema, second, "\tchair-1 \n", true);

            assertThrows(FlywayException.class, () -> migrate(schema, null));
            assertEquals("Chair-1", value(schema, "sku", first, String.class));
            assertEquals("\tchair-1 \n", value(schema, "sku", second, String.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns"
                    + " WHERE table_schema = ? AND table_name = 'products' AND column_name = 'status'",
                    Integer.class, schema));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + schema
                    + ".flyway_schema_history WHERE version = '3'", Integer.class));
        });
    }

    @Test
    void unsupportedLegacyWhitespaceAbortsUpgradeWithoutChangingSku() {
        withSchema(schema -> {
            migrate(schema, "2");
            UUID id = UUID.randomUUID();
            String sku = "\u001cLEGACY-1";
            insertLegacy(schema, id, sku, false);

            assertThrows(FlywayException.class, () -> migrate(schema, null));
            assertEquals(sku, value(schema, "sku", id, String.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + schema
                    + ".flyway_schema_history WHERE version = '3'", Integer.class));
        });
    }

    private void insertLegacy(String schema, UUID id, String sku, boolean archived) {
        jdbc.update("INSERT INTO " + schema + ".products"
                        + " (id, sku, name, description, price, archived, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, now(), now())",
                id, sku, "Legacy product", "Preserved description", new BigDecimal("12.50"), archived);
    }

    private void insertProduct(String schema, UUID id, String sku, String status, int weight, String length) {
        jdbc.update("INSERT INTO " + schema + ".products"
                        + " (id, sku, name, price, status, version, weight_grams, length_cm, width_cm, height_cm, created_at, updated_at)"
                        + " VALUES (?, ?, ?, 12.50, ?, 0, ?, ?, 2, 3, now(), now())",
                id, sku, "New product", status, weight, new BigDecimal(length));
    }

    private void insertConcurrent(Connection connection, String schema, String sku) throws SQLException {
        try (var statement = connection.prepareStatement("INSERT INTO " + schema + ".products"
                + " (id, sku, name, price, status, created_at, updated_at)"
                + " VALUES (?, ?, 'Race', 12.50, 'DRAFT', now(), now())")) {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, sku);
            statement.executeUpdate();
        }
    }

    private int backendPid(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT pg_backend_pid()")) {
            result.next();
            return result.getInt(1);
        }
    }

    private void assertBlockedBy(int blockedPid, int blockerPid) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            Boolean blocked = jdbc.queryForObject(
                    "SELECT ? = ANY(pg_blocking_pids(?))", Boolean.class, blockerPid, blockedPid);
            if (Boolean.TRUE.equals(blocked)) return;
            Thread.yield();
        }
        org.junit.jupiter.api.Assertions.fail("Second insert never waited for the first transaction's SKU lock");
    }

    private <T> T value(String schema, String column, UUID id, Class<T> type) {
        return jdbc.queryForObject("SELECT " + column + " FROM " + schema + ".products WHERE id = ?", type, id);
    }

    private void migrate(String schema, String target) {
        var config = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .defaultSchema(schema).schemas(schema);
        if (target != null) config.target(target);
        config.load().migrate();
    }

    private void withSchema(java.util.function.Consumer<String> test) {
        String schema = "catalog_v3_" + UUID.randomUUID().toString().replace("-", "");
        try {
            test.accept(schema);
        } finally {
            jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }
}
