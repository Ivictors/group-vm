# Catalog service

Spring Boot service for product registration, listing, updates, and archiving. It owns its product data in PostgreSQL.

## Architecture

| Package | Responsibility |
| --- | --- |
| `resource` | HTTP endpoints, request/response DTOs, and error responses |
| `application` | Product use cases and business flow |
| `domain` | `Product` entity and its rules |
| `infra` | Spring Data JPA repository |

Requests flow through `ProductResource` → `ProductService` → `ProductRepository` → PostgreSQL. Flyway manages the database schema.

## Tests

From this directory, run `mvn test` for unit and resource tests. For integration tests, start an isolated `postgres:18.6` container bound to the local loopback interface, set `CATALOG_DB_URL`, `CATALOG_DB_USERNAME`, and `CATALOG_DB_PASSWORD` in your terminal, then run `mvn test -Pintegration`. Run it again against the same container to check an already migrated database. Keep credentials local.

## Migrations

The validated local combination is Spring Boot `3.5.7`, Flyway `11.19.1`, and PostgreSQL `18.6`. Spring Boot manages Flyway `11.7.2` by default; this service overrides it. [Spring Boot dependencies](https://docs.spring.io/spring-boot/3.5/appendix/dependency-versions/coordinates.html) and [Flyway database support](https://documentation.red-gate.com/flyway/getting-started-with-flyway/system-requirements/supported-databases-and-versions) were checked on 2026-11-14. Revalidate the combination after version changes.

Before applying V2 to existing data, check for blank SKUs with `SELECT id, sku FROM products WHERE length(btrim(sku)) = 0;` and correct any result through an approved data change. PostgreSQL rolls back a failed V2 migration. After a successful migration, use a reviewed corrective migration when needed; restoring a backup and the previous application version requires a coordinated recovery window and may discard later writes.
