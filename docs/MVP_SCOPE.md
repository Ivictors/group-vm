# Essential commerce MVP

## Goal

Demonstrate one complete purchase journey for physical products: an operator publishes a product, a guest adds products to a server-side cart, confirms delivery and the current total, completes sandbox payment, and receives an order status that can be consulted later.

Market intelligence is a later product line. It is not required for the first commerce release.

## Essential deployable services

| Service | Owns | First release responsibilities |
|---|---|---|
| `catalog-service` | Products, SKU, prices, shipping attributes, and catalog lifecycle | Administrative product creation and lifecycle; active product reads for the storefront |
| `commerce-service` | Guest carts, customer/order snapshots, checkout state, and transactional notifications | Cart retention, guest checkout, shipping selection, order orchestration, order lookup, and fulfillment status |
| `inventory-service` | Availability, reservations, and movements | Idempotent reservations with TTL, complete reservation confirmation, release, and concurrency protection |
| `payment-service` | Payment attempts and webhook state | Sandbox hosted checkout, authenticated webhook handling, idempotency, and reconciliation state |

The frontend is a client application, not a business service. The first deployment may run these four services and their databases on one execution platform while preserving database ownership and HTTP contracts.

## Responsibilities kept inside `commerce-service` initially

- Guest contact and delivery data needed by an order.
- Shipping provider adapters and quote validity.
- Email delivery adapter for temporary order lookup codes and transactional messages.
- Checkout orchestration and the order state machine.

These responsibilities may become independent services only after measured operational or ownership pressure justifies the split. They must remain separate modules and ports inside `commerce-service` so extraction does not require moving business rules from unrelated packages.

## Deferred services and infrastructure

- `collector-service`, `market-intelligence-service`, and `ai-assistant-service` are outside the commerce MVP.
- Independent `customer-service`, `shipping-service`, and `notification-service` deployments are deferred; their initial responsibilities remain in `commerce-service`.
- Kafka is deferred until an approved event flow requires it. The first synchronous purchase path uses versioned HTTP contracts and idempotency keys.
- Keycloak/OIDC is required before releasing administrative routes. Local and test profiles may use explicit test security configuration; production must deny administrative access until the security issue is complete.
- Kong and Kubernetes/Helm remain deployment options, not prerequisites for local development or unit tests.

## Purchase flow

```text
Angular storefront
        |
        v
commerce-service
   |       |        |
   v       v        v
catalog inventory payment
```

1. The guest reads active products from `catalog-service` and maintains a cart through `commerce-service`.
2. `commerce-service` obtains or calculates a valid shipping option and shows the current total.
3. After explicit confirmation, it creates an order in `PENDING_RESERVATION` and requests idempotent reservations from `inventory-service`.
4. Only a complete reservation moves the order to `PENDING_PAYMENT` and allows `payment-service` to start hosted checkout.
5. An authenticated payment webhook confirms or rejects the attempt. Replays do not duplicate payment or inventory effects.
6. A reservation failure releases every partial reservation and keeps the cart available for correction.

## Delivery order

1. Catalog domain, persistence, lifecycle, public read, and administrative HTTP contract.
2. `inventory-service` reservation contract and isolated tests.
3. `commerce-service` cart and order state machine with catalog, inventory, shipping, and payment ports.
4. `payment-service` sandbox adapter and webhook contract.
5. Security, observability, failure recovery, and end-to-end purchase evidence.

The corresponding backlog is catalog issues #21–#26, inventory issue #27, commerce issue #28, and payment issue #29. Issue #10 supplies per-service CI; #12 supplies the identity controls required before administrative release. Issue #11 remains deferred until an event flow is approved.

Every service keeps its own database and tests. Cross-service tests use HTTP contracts or test doubles; they do not import another service's entities or repositories.
