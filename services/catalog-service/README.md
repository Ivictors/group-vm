# Catalog service

Spring Boot service for administrative product drafts, activation, updates, and archiving, plus public reads of active products. It owns its product data in PostgreSQL.

## Architecture

| Package | Responsibility |
| --- | --- |
| `resource` | HTTP endpoints, request/response DTOs, and error responses |
| `application` | Product use cases and business flow |
| `domain` | `Product` entity and its rules |
| `infra` | Spring Data JPA repository |

Requests flow through an HTTP resource, application service, and repository to PostgreSQL. Flyway manages the database schema.

Public `GET /api/v1/products` and `GET /api/v1/products/{productId}` are available in all profiles. PostgreSQL filters them to `ACTIVE` products; unknown, draft, and archived IDs return the same 404 response. The list uses zero-based `page` (default 0) and `size` (default 20, maximum 100), ordered by `createdAt` descending and then `id` descending. List items contain only ID, name, and price; detail also contains description and shipping measurements. The OpenAPI file is the response contract.

## Tests

From this directory, run `mvn test` for unit and resource tests. For integration tests, start an isolated `postgres:18.6` container bound to the local loopback interface, set `CATALOG_DB_URL`, `CATALOG_DB_USERNAME`, and `CATALOG_DB_PASSWORD` in your terminal, then run `mvn test -Pintegration`. Run it again against the same container to check an already migrated database. The integration suite uses temporary schemas for migration scenarios and deletes them afterward. Keep credentials local; never connect these tests to a shared database.

The administrative HTTP controller is enabled only with the `local`, `test`, or `integration` Spring profile until service-side authorization is implemented in issue #38. Do not expose one of these profiles outside an isolated development or test environment.

## Migrations

The validated local combination is Spring Boot `3.5.7`, Flyway `11.19.1`, and PostgreSQL `18.6`. Spring Boot manages Flyway `11.7.2` by default; this service overrides it. [Spring Boot dependencies](https://docs.spring.io/spring-boot/3.5/appendix/dependency-versions/coordinates.html) and [Flyway database support](https://documentation.red-gate.com/flyway/getting-started-with-flyway/system-requirements/supported-databases-and-versions) were checked on 2026-11-14. Revalidate the combination after version changes.

Before applying V2 to existing data, check for blank SKUs with `SELECT id, sku FROM products WHERE length(btrim(sku)) = 0;` and correct any result through an approved data change. PostgreSQL rolls back a failed V2 migration.

Before applying V3, take a verified backup and inspect SKU collisions using the same normalization as the migration:

```sql
SELECT lower(regexp_replace(sku, '^[[:space:]]+|[[:space:]]+$', '', 'g')) AS normalized_sku,
       count(*), array_agg(id)
FROM products
GROUP BY 1
HAVING count(*) > 1;
```

Resolve collisions through an approved data correction before deployment. V3 trims surrounding whitespace from legacy SKUs, preserves their identifiers, descriptions, prices, and timestamps, maps `archived=false` to `DRAFT` and `archived=true` to `ARCHIVED`, and leaves unknown shipping measurements empty. PostgreSQL rolls back V3 completely if a constraint or unique index fails. The index also prevents case-insensitive SKU duplication after migration. New products require positive shipping measurements and a positive BRL price with two decimal places; activation requires all measurements.

Java 21 uses `String.strip()` for incoming SKUs. PostgreSQL's POSIX whitespace class used by V3 does not trim U+001C through U+001F, so V3 rejects any legacy SKU containing those control characters instead of keeping an inconsistent value; the database constraint also rejects new rows containing them. Check for them before deployment with `SELECT id FROM products WHERE position(chr(28) IN sku) > 0 OR position(chr(29) IN sku) > 0 OR position(chr(30) IN sku) > 0 OR position(chr(31) IN sku) > 0;`. Recheck the normalization behavior when changing Java or PostgreSQL versions.

After a successful V3 migration, the older application is incompatible because the `archived` column has been removed. Prefer a reviewed forward corrective migration. Restoring the pre-V3 backup and the older application requires a coordinated recovery window, and writes after that backup need reconciliation. Test backup restoration before production use.
