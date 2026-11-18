# Catalog service

## Local integration test

The integration profile uses PostgreSQL through Docker. It does not install PostgreSQL locally and does not use Testcontainers.

Start an isolated database using the local `postgres:18.6` image:

```powershell
$env:POSTGRES_PASSWORD = Read-Host "Choose a password for this disposable database"
$loopbackAddress = [System.Net.IPAddress]::Loopback.ToString()
docker run --rm --name group-vm-catalog-postgres `
  -e POSTGRES_DB=catalog_integration `
  -e POSTGRES_USER=catalog_app `
  -e POSTGRES_PASSWORD `
  -p "${loopbackAddress}:55432:5432" `
  postgres:18.6
```

In another terminal, enter the same password and run:

```powershell
$env:CATALOG_DB_URL = "jdbc:postgresql://localhost:55432/catalog_integration"
$env:CATALOG_DB_USERNAME = "catalog_app"
$env:CATALOG_DB_PASSWORD = Read-Host "Enter the same local database password"
mvn test -Pintegration
```

Choose a password only for this disposable local database. The commands read it at runtime; do not add the value to this file or commit it. The port is bound to the local loopback interface, so the database is not published on external interfaces.

The profile applies Flyway migrations and runs the HTTP CRUD scenarios against this disposable database. Run the command again before stopping the container to verify an already migrated database. The tests also migrate an isolated schema from V1 to V2 with an existing product, verify the new constraint, and check that a failed V2 migration leaves V1 data intact. Stop the container with `Ctrl+C` or `docker rm -f group-vm-catalog-postgres`.

The service uses Spring Boot `3.5.7`, Flyway `11.19.1`, and the `postgres:18.6` integration image. Spring Boot 3.5 manages Flyway `11.7.2` by default; the explicit Flyway version keeps the Spring Boot 3.5 API compatibility and was validated against PostgreSQL 18.6 here. Redgate lists PostgreSQL 18 among its supported databases, but that page does not certify this exact Flyway patch release. The approved project combination is therefore backed by the integration tests above and must be revalidated when either version changes. These versions and references were reviewed on 2026-11-14: [Spring Boot managed dependencies](https://docs.spring.io/spring-boot/3.5/appendix/dependency-versions/coordinates.html), [Flyway supported databases](https://documentation.red-gate.com/flyway/getting-started-with-flyway/system-requirements/supported-databases-and-versions).

Before applying V2 to an existing database, check for incompatible rows with `SELECT id, sku FROM products WHERE length(btrim(sku)) = 0;`. Resolve any result through an approved data correction; V2 must not silently rewrite SKUs. PostgreSQL rolls back the failed V2 DDL transaction, leaving V1 and its data in place, as the integration test verifies.

Migrations are forward-only. After a successful migration in a persistent environment, prefer a reviewed corrective migration if live data has changed. Restoring a pre-migration backup and deploying the previously approved application version requires a coordinated recovery window and may discard later writes. A full backup/restore drill belongs to the catalog readiness issue; this issue validates failed-migration rollback only. Local integration databases can be discarded and recreated with the Docker command above.
