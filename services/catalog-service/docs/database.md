# Database setup and migrations

## Local integration test

Use the `postgres:18.6` Docker image. In one PowerShell terminal, start an isolated database:

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

Choose a password only for this disposable database; do not commit it. The port is bound to the local loopback interface. Run the test command again before stopping the container to validate an already migrated database. Stop the container with `Ctrl+C` or `docker rm -f group-vm-catalog-postgres`.

The integration tests exercise HTTP product operations, upgrade an isolated schema from V1 to V2 while retaining an existing product, reject a blank SKU, and verify that a failed V2 migration leaves V1 data intact.

## Version and recovery

The service uses Spring Boot `3.5.7`, Flyway `11.19.1`, and PostgreSQL `18.6` for local integration. Spring Boot 3.5 manages Flyway `11.7.2` by default; this service overrides it. Redgate lists PostgreSQL 18 as supported but does not certify this exact Flyway patch on its general support page. The local integration tests validate this combination; revalidate it after a version change. References checked on 2026-11-14: [Spring Boot managed dependencies](https://docs.spring.io/spring-boot/3.5/appendix/dependency-versions/coordinates.html) and [Flyway supported databases](https://documentation.red-gate.com/flyway/getting-started-with-flyway/system-requirements/supported-databases-and-versions).

Before applying V2 to an existing database, run `SELECT id, sku FROM products WHERE length(btrim(sku)) = 0;`. Resolve any result through an approved data correction; V2 does not rewrite SKUs. PostgreSQL rolls back a failed V2 DDL transaction, leaving V1 data in place, as the integration test verifies.

Migrations are forward-only. After a successful migration on persistent data, use a reviewed corrective migration when needed. Restoring a pre-migration backup and deploying the previously approved application version requires a coordinated recovery window and may discard later writes. A full backup/restore drill belongs to catalog readiness; this issue validates failed-migration rollback only. The disposable local database can be recreated with the Docker command above.
