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

From this directory, run unit and resource tests with `mvn test`. Integration tests use an isolated PostgreSQL container and run with `mvn test -Pintegration`. See [database setup and migrations](docs/database.md) for the Docker commands and migration checks.
