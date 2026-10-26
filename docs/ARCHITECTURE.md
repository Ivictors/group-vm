# Architecture and contracts — VmGriffes

## Current decision
Operational monorepo with independent microservices: each service has its own `pom.xml`, code, tests, database, and build/deployment lifecycle. There is no multi-module parent POM or shared domain library. Services communicate only through HTTP/REST documented with OpenAPI or versioned Kafka events. No service reads another service's database. Independence also applies to tests: contract mocks are allowed, but imports of another service's entities and repositories are not. This choice increases the cost of contracts, operations, and distributed observability.

## Services (source of truth)
| Service | Initial stack | Owned data | MVP |
|---|---|---|---|
| catalog-service | Spring Boot | products, categories, published prices, shipping attributes | yes |
| inventory-service | Spring Boot | balances, reservations, movements | yes |
| order-service | Spring Boot | orders, snapshots, state, checkout, fulfillment, and after-sales requests | yes |
| payment-service | Spring Boot | attempts, transactions, webhooks | yes |
| collector-service | Quarkus | authorized sources, collection runs, normalized payloads | yes |
| market-intelligence-service | Spring Boot | references, opportunities, policies | yes |
| cart-service | Spring Boot | guest carts and inactivity expiration | yes |
| customer-service | Spring Boot | guest contact and delivery address; reusable profiles later | yes |
| shipping-service | Spring Boot | admin-configured CEP ranges, quotes, validity, and two-provider integration | yes |
| ai-assistant-service | Quarkus | nonauthoritative sessions/context | later |
| notification-service | Quarkus | temporary code delivery by email; campaigns later | yes |

**Frontend and edge:** Angular in the MVP for storefront, cart, shipping, guest checkout, order lookup, and minimal administration. Kong is the infrastructure gateway and owns no business data. It does not replace authorization within services.

**Infrastructure:** Keycloak/OIDC for identity; PostgreSQL with separate databases and credentials per service (the same physical server is allowed in development); Kafka KRaft for events, with topics and ACLs defined by contract; sandbox PSP. Docker Compose may support local execution and integration; Kubernetes/Helm is the target deployment architecture. Observability evolves through logs, metrics, and OpenTelemetry.

**Deployment:** `docs/DEPLOYMENT_PLAN.md` surveys static frontend hosting, VPS/Compose, Coolify, Dokploy, managed alternatives, databases, costs, and operations. That study replaces none of the components or contracts above and does not yet select a provider or first execution platform. With the current R$ 100/month ceiling for the entire operation, do not accept real orders until a complete, available, and recoverable deployment is demonstrated within the then-approved budget. Implementing the MVP and launching the store are separate milestones.

## Cart, shipping, and checkout flow
1. `cart-service` keeps guest items and quantities for seven days without activity, linked to an opaque identifier in the same browser. The cart neither reserves inventory nor guarantees price or availability.
2. `customer-service` receives guest contact and address details without requiring an account. `shipping-service` validates the CEP range, queries the first provider, and tries the second if the first fails or offers no service; it returns options with price, delivery time, and validity. Checkout cannot proceed without a valid option.
3. The buyer chooses shipping. `order-service` reconfirms items, quantities, prices, availability, and the quote before final confirmation. Changes are shown to the buyer; an expired or changed quote is requested again and reconfirmed. Contract formats and treatment of unavailability belong to F03a.
4. After explicit confirmation and with an `Idempotency-Key`, `order-service` creates an order in `PENDING_RESERVATION` with immutable snapshots of items, amounts, shipping, and the delivery details needed for the order. Reusable guest data remain owned by `customer-service`.
5. `order-service` requests idempotent reservations from `inventory-service` for all lines and quantities, with a TTL. Only a complete reservation moves the order to `PENDING_PAYMENT` and allows payment for the item total plus shipping to start. If any line fails, all partial reservations are released, payment does not start, and the buyer may remove items and confirm a new attempt. Release and reconciliation of partial failures are mandatory.
6. `payment-service` starts hosted checkout. The PSP sends an authenticated, idempotent webhook; `payment-service` records state plus outbox and publishes `payment.succeeded.v1` or `payment.failed.v1`.
7. `order-service` consumes the event, confirms or cancels the order, and requests reservation `commit` or `release`. Retries and reconciliation handle partial failures; `REQUIRES_RECONCILIATION` identifies inconclusive cases. Purchased items leave the cart after completion; a failure preserves the cart for correction.
8. `order-service` controls order status, dispatch, tracking, and the history of cancellation/return requests. For lookup without an account, it generates and verifies a temporary challenge; `notification-service` only delivers the code by email. Neither public responses nor logs may reveal the existence of an order by email address. Refunds and detailed eligibility remain pending contract and operational decisions.
9. No service accesses another service's database. Every inventory, order, or payment mutation with repeatable effects is idempotent.

## Contracts
HTTP: `/api/v1/...`, Problem Details RFC 9457 for errors, explicit versioning, timeouts, and a correlation ID. OpenAPI per service, including cart, guest contact, quotes, multi-item checkout, code-based lookup, and administrative fulfillment/after-sales operations. F03a defines exact formats and limits.
Kafka events: an envelope with `eventId` (UUID), `eventType`, `schemaVersion`, `aggregateId`, `occurredAt`, `correlationId`, and `payload`; the `domain.action.v1` convention and compatible additive fields. Incompatible changes require a new event. Transactional producers use outbox; consumers use deduplication/inbox. Define topics, keys/ordering, producer acknowledgment, retry policy, invalid-message handling, retention, and replay by contract before integration. Choosing the broker alone provides none of these guarantees.

## Security
Each service verifies JWT/OIDC on authenticated routes; guest checkout and code-based lookup are public flows with opaque identifiers, authorization through possession/verification, and attempt limits. The gateway does not replace authorization within services. Service-to-service authentication uses OAuth client credentials or equivalent. Keep secrets in a secret manager/environment. Do not log personal data or codes. See `docs/SECURITY.md`.

## Collection
The collector uses a connector for an explicitly permitted API, feed, or source; it contains no bypass of access controls. Market Intelligence receives normalized facts and does not control the catalog. Commercial publication occurs only after a purchase/supply contract and human validation.

## Engineering boundaries
- Do not share JPA entities, business DTO libraries, migrations, or database schemas.
- Tests may simulate external services by contract; they do not import another service's domain or persistence code.
- No distributed 2PC transactions in the MVP; use a saga with compensations.
- No service calls another through its database, filesystem, or direct imports.
- Producer/consumer contract tests and E2E tests for essential flows.
- `Quarkus native` is an optional, measured optimization, not a requirement; consider build time and compatibility.
- No service depends on Kubernetes to run locally; unit tests do not depend on Kafka.

## Decision summary (without individual ADRs)
D01 Monorepo with independent builds: simplifies governance and preserves service isolation.
D02 HTTP for queries and reservations; Kafka KRaft for payment facts and asynchronous events: predictable checkout and decoupling.
D03 Database per service: data ownership and independent evolution.
D04 External Keycloak: avoid reinventing credential security.
D05 Sandbox PSP and hosted checkout: reduce PCI scope.
D06 Spring Boot for transactional services; Quarkus for workers: initial choice subject to benchmarking.
D07 Angular in the MVP; Kong at the edge, with authorization retained in services.
D08 Kubernetes/Helm as target deployment architecture; select the first execution platform based on budget and measurements, without requiring a local cluster.
D09 One branch per issue in the project directory; distributed integration in CI/a remote environment when needed.
D10 JaCoCo, Spotless, Gitleaks, tests, and static analysis are controls to implement in the pipeline, not gates that already exist.
D11 Guest cart, guest contact, shipping with two providers, and minimal email delivery belong to the MVP; their responsibilities remain in independent services. Intelligence remains in the same MVP.

## Superseded decisions and open items
The initial queue-broker choice was replaced by Kafka KRaft. The initially planned Java gateway was replaced by Kong. The idea of developing with the complete local infrastructure was replaced by running only the required components. Current product scope and service responsibilities are defined here and in `docs/PRODUCT.md`; check each change's scope in its approved GitHub Issue.

The initial version baseline and local tool audit are recorded in `docs/REFERENCES.md`; each implementation issue must recheck patch releases and compatibility before pinning dependencies or images. Kong Community/OSS maintenance needs confirmation before production exposure. The final PSP, two shipping providers, RBAC matrix, reservation/quote/code TTLs, timeouts, reconciliation, cancellation/return/refund policy, final hosting choice, validated backup strategy, and retention remain open. The cart TTL is already decided: seven days without activity. The planned stack is summarized in `docs/.specs/_stack.json`.

## References
https://docs.spring.io/spring-boot/reference/
https://quarkus.io/guides/
https://kafka.apache.org/documentation/
https://www.keycloak.org/documentation
https://www.rfc-editor.org/rfc/rfc9457
https://angular.dev/overview
https://developer.konghq.com/gateway/
https://kubernetes.io/docs/
