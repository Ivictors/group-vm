# Catalog service

Spring Boot service for administrative product drafts, activation, updates, and archiving, plus public reads of active products. It owns its product data in PostgreSQL.

## Architecture

| Package | Responsibility |
| --- | --- |
| `resource` | HTTP endpoints, request/response DTOs, and error responses |
| `application` | Product use cases and business flow |
| `domain` | `Product` and immutable `PriceHistory` records |
| `infra` | Spring Data JPA repositories |

Requests flow through an HTTP resource, application service, and repository to PostgreSQL. Flyway manages the database schema.

Public `GET /api/v1/products` and `GET /api/v1/products/{productId}` are available in all profiles. PostgreSQL filters them to `ACTIVE` products; unknown, draft, and archived IDs return the same 404 response. The list uses zero-based `page` (default 0) and `size` (default 20, maximum 100), ordered by `createdAt` descending and then `id` descending. List items contain only ID, name, and price; detail also contains description and shipping measurements. The OpenAPI file is the response contract.

## Tests

From this directory, run `mvn test` for unit and resource tests. For integration tests, start an isolated `postgres:18.6` container bound to the local loopback interface, set `CATALOG_DB_URL`, `CATALOG_DB_USERNAME`, and `CATALOG_DB_PASSWORD` in your terminal, then run `mvn test -Pintegration` twice against the same container to check a fresh and an already migrated database. Finish with `mvn package -DskipTests`; the tests have already run. The integration suite uses temporary schemas for migration scenarios and deletes them afterward. Keep credentials local; never connect these tests to a shared database. CI runs these commands with Java 21 and uploads Surefire reports; check the `catalog` job on PRs to `develop`.

Set `CATALOG_JWT_ISSUER_URI` to the Keycloak realm issuer before starting the service. Startup fails without it. The service verifies JWT signature, issuer, audience `catalog-service`, expiry, not-before time, and a nonblank subject. Keycloak must put the `catalog_admin` client role in `resource_access.catalog-service.roles` and include `catalog-service` in the access token audience. If issuer discovery is unavailable at startup, an operator may set `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` to that realm's JWK endpoint; issuer validation still applies. Do not commit tokens, credentials, or environment-specific addresses.

Only anonymous `GET /api/v1/products` and `GET /api/v1/products/{productId}` are allowed. Administrative routes require a valid token with `catalog_admin`; other routes are denied. Missing or invalid tokens return 401, and a valid token without permission returns 403. Product creation and price changes record the validated token subject as the actor. Client-supplied actor fields are rejected. Administrative product responses include price history, ordered oldest first; public responses do not expose it. Local tests use generated keys and a loopback JWK server; they do not need a running Keycloak instance.

## Operations

`GET /actuator/health/liveness` reports whether the process should stay running. `GET /actuator/health/readiness` also checks PostgreSQL; it returns 503 when the database indicator is down, while liveness can remain healthy. Both are anonymous and return only an overall status. Other Actuator paths, including metrics and the aggregate health endpoint, are denied. Locally, check them with `curl http://localhost:8081/actuator/health/liveness` and `curl http://localhost:8081/actuator/health/readiness`. If readiness is down, inspect database availability and connection settings, restore the dependency, and verify readiness before directing traffic to the service; do not restart a healthy process repeatedly to mask a database outage.

Every request receives a server-generated `X-Correlation-ID`; client-supplied values are ignored. Problem responses repeat the identifier as `correlationId`; a 406 response has no body but still has the header. Operators can use the identifier to match an error to its log entry. Failure logs include only the identifier, HTTP method, status, and duration; they omit paths, queries, bodies, tokens, and personal data. Metrics, dashboards, and alerting are outside this increment.

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

V4 adds an append-only price history table. It deliberately does not backfill old products: their existing price remains intact, and their history starts with the first authenticated price change. No historical actor is invented. A database trigger rejects updates and deletes of recorded entries. Product and history writes share one PostgreSQL transaction; a failed write or optimistic version conflict rolls both back. Before deployment, take a verified backup and retain the V3 application image. To recover from a failed V4 deployment, prefer a reviewed forward migration; restoring the pre-V4 backup requires a coordinated application rollback and reconciliation of later writes.
