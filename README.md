# VmGriffes — own-store commerce and market intelligence

Foundation for a monorepo with independent Java microservices, an Angular frontend, own-store commerce, and market intelligence.

## Product
- Commerce: catalog, guest cart, delivery details, shipping, inventory, orders, payments, secure order lookup, fulfillment, and after-sales requests.
- Intelligence: authorized collection of product and price data, comparison, and commercial analysis.
- Angular frontend in the MVP: storefront, cart, shipping, guest checkout, order lookup, and minimal administration.
- Integration through HTTP/REST with OpenAPI and versioned Kafka events. No Java dependencies between services, including tests, and no shared databases.

## MVP
Commerce flow: server-side cart retained for seven days of inactivity, without inventory reservation -> guest details and shipping quote with fallback between two providers -> confirmation of prices, availability, and shipping -> order in `PENDING_RESERVATION` -> complete reservation of all items -> `PENDING_PAYMENT` -> sandbox PSP -> authenticated webhook -> order confirmation and a single inventory deduction. If any line cannot be reserved, release all partial reservations and block payment; the buyer corrects the cart and confirms a new attempt.
After purchase, the guest looks up the order using a temporary code sent by email; an admin records dispatch/tracking and reviews cancellation/return requests. Intelligence is also part of the MVP.
Intelligence pipeline: authorized input -> normalization -> storage -> opportunity calculation -> administrative lookup.

## Architecture and execution
- Spring Boot for transactional services, including cart/customer/shipping, and Quarkus for the collector and notification service; each service has its own build, data, and tests.
- Kong as the infrastructure gateway; Keycloak/OIDC for identity; PostgreSQL with a separate database and credentials per service.
- Kubernetes/Helm as the target deployment architecture. Development does not require a local cluster; Kafka/Kong/Keycloak are started only when a task requires them.
- `docs/DEPLOYMENT_PLAN.md` compares hosting, costs, and operations without selecting a provider or authorizing deployment. With the current R$ 100/month ceiling for the entire operation, real orders remain postponed until a complete, available, and recoverable solution is demonstrated within the approved budget.
- Current CI checks only the foundation. Full integration tests and JaCoCo, Spotless, Gitleaks, and static-analysis gates remain to be implemented in the relevant issues.

## Governance files
- `AGENTS.md`: AI agent working rules and blockers.
- `CONTRIBUTING.md`: GitHub Issues, one branch per issue, PRs, quality, and Definition of Done.
- `docs/SECURITY.md`: security and collection policies.
- `docs/PRODUCT.md`: MVP requirements, boundaries, and acceptance criteria.
- `docs/ARCHITECTURE.md`: services, domain, contracts, and decisions.
- `docs/DEPLOYMENT_PLAN.md`: options and criteria for future deployment, backups, costs, and commercial launch.

## Getting started
Read the requirements in `docs/PRODUCT.md`, the decisions in `docs/ARCHITECTURE.md`, and the contribution workflow in `CONTRIBUTING.md`.
Create one branch per issue in the project directory, as described in `CONTRIBUTING.md`.
Implement and validate services incrementally. Current CI checks only foundation files.

### Local PostgreSQL for catalog development

The committed `infra/postgres/compose.example` can start only PostgreSQL 18.6 on `127.0.0.1:5433` with `docker compose -f infra/postgres/compose.example up -d --wait postgres`, once its ignored local password files under `infra/postgres/secrets/` exist. The catalog password file is mounted for future provisioning; this example does not create a catalog database or role. The ignored `infra/postgres/compose.yaml` is not used or changed.

Run `docker compose -f infra/postgres/compose.example ps` to check health. Run `docker compose -f infra/postgres/compose.example down` to stop the container while retaining its named volume; do not use `down -v` when preserving local data. The ignored root `.env` has `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` for future catalog-service work. Never commit or print these files or values.

On this notebook, the healthy container used about 25 MiB in one `docker stats --no-stream` sample; this is an observation, not a memory limit. A busy port 5433, unavailable Docker daemon, missing image/network access, or absent local password files prevent startup. Repeatable catalog database provisioning remains issue #8; catalog service code and its real connection test are tracked in issue #9.

## References
- https://docs.spring.io/spring-boot/reference/
- https://quarkus.io/guides/
- https://docs.github.com/en/actions
- `docs/REFERENCES.md` contains the initial version baseline, local tool audit, and additional official references. Selected versions are rechecked when each component is implemented.
