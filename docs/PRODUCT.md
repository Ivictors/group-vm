# Product, requirements, and acceptance criteria

## Goal and users
Operate an own-store ecommerce business for physical products and support sourcing/pricing with legitimate market data. Commerce and Intelligence belong to the same MVP. Users: guest buyer, catalog/inventory and order operators, opportunity analyst, and administrator. A buyer account is not required.

## Commerce MVP
RF01 An admin creates and activates products with a SKU, traceable price, and attributes needed for shipping quotes. Exact variants and attributes will be defined in F03a/C01.

RF02 Inventory exposes availability and reserves quantities per item with a TTL, idempotency, and concurrency protection. Adding or keeping products in the cart does not reserve inventory.

RF03 Idempotent checkout accepts multiple products and quantities. After explicit confirmation of current items, prices, and shipping, it creates an order with monetary and delivery snapshots in `PENDING_RESERVATION`. It reserves all items before moving to `PENDING_PAYMENT` and starting payment. If any item fails, it releases every reservation obtained, identifies the unavailable item, and blocks payment; the buyer may return to the product, remove items, and confirm a new attempt, without an implicit retry.

RF04 Payment uses a sandbox PSP and hosted checkout; an authenticated webhook confirms or rejects payment. The authorized total includes items and the chosen shipping option. A browser redirect does not confirm payment.

RF05 A confirmed order consumes reservations exactly once; failure or expiration releases them. Partial failures must be identifiable and reconcilable.

RF06 An admin views orders and transitions and records dispatch and a tracking code. A guest views order status without an account using a temporary code sent to the order email address for each access.

RF07 The collector imports data from an authorized or fictional source and normalizes products and prices.

RF08 Intelligence calculates price references, risk, and margin; it does not buy or publish products automatically.

RF09 The gateway exposes only the necessary public endpoints and protects administrative endpoints; each service also enforces authorization. Public guest flows do not require login.

RF10 Angular provides a storefront, product details, cart, guest identification, shipping, checkout, order lookup, and a minimal admin view for catalog, CEP coverage, orders, fulfillment, and after-sales requests. The E2E journey must be demonstrable through the UI and APIs.

RF11 The guest cart is stored on the server and can be resumed in the same browser for seven days without activity; cart activity resets this period. It accepts multiple products and quantities. It does not guarantee prices or availability. After a completed order, purchased items leave the cart; a failed checkout preserves items for correction.

RF12 Before reservation, price or availability changes since an item was added to the cart are shown and require explicit confirmation. Unavailable quantities must be adjusted; changed values are not silently accepted.

RF13 The store configures supported CEP ranges. A guest provides a name, email, phone number, and address. `customer-service` owns these data without requiring an account.

RF14 `shipping-service` obtains quotes from two configured shipping providers. If the first fails or does not serve the CEP, it automatically tries the second. The buyer chooses an option after seeing its price and delivery time; confirmation is blocked without a valid quote. An expired or changed quote is requested again before final confirmation and requires the buyer to confirm again.

RF15 `notification-service` delivers the temporary lookup code by email. Requesting or verifying the code must not reveal whether an email address has orders; attempts and sends are rate limited. Exact validity periods and limits belong in the contracts and implementation issue.

RF16 From the order, the buyer requests cancellation or a return; an admin reviews the request and records a decision and history. The request does not automatically cancel the order or issue a refund. Eligibility, deadlines, and refund execution must be defined before processing real orders.

## Outside the MVP
Optional login and reusable buyer profiles, AI, Telegram, multiple payment providers, automatic publication of third-party listings, advanced administration, and multi-region high availability. Minimal transactional email belongs to the MVP; campaigns and additional notifications come later.

## Commerce E2E acceptance
- Cart: multiple items and quantities, resumed in the same browser within seven days of activity and expired after seven days without activity; adding items does not change reservations.
- Updates: price, availability, or shipping changes are shown and reconfirmed before reservation; an unavailable quantity blocks the attempt until corrected.
- Shipping: a CEP outside supported ranges is rejected; failure or lack of service from the first provider triggers the second; the buyer chooses a price and delivery time; checkout cannot be confirmed without a valid quote; an expired quote is refreshed and reconfirmed.
- Success: a multi-item order starts in `PENDING_RESERVATION`; all items are reserved; it moves to `PENDING_PAYMENT`; a valid webhook confirms sandbox payment; the order and inventory are confirmed exactly once.
- Reservation failure: if an item fails, all partial reservations are released, payment does not start, the unavailable item is identified, the buyer can return to the product, and a new attempt occurs only after buyer confirmation.
- Payment failure: refusal or expiration cancels/expires the order and releases reservations; webhook/event replay does not duplicate charges or inventory deductions.
- Concurrency: simultaneous purchases do not make inventory negative or allow payment without a valid complete reservation.
- Access and fulfillment: a guest looks up an order after receiving and verifying a temporary email code; another guest cannot access those data; an admin records dispatch and tracking.
- After-sales: the buyer requests cancellation or a return from the order; an admin records the review and decision without an automatic refund.
- Frontend: the buyer completes the storefront, cart, shipping, checkout, and order lookup journey in Angular; an authorized operator uses the minimal admin view.

## Intelligence E2E acceptance
An authorized source produces normalized and deduplicated data, a calculated opportunity, and an administrative lookup; an unauthorized source does not run. Intelligence does not automatically change the catalog, prices, inventory, or orders.

## Nonfunctional requirements
Versioned API; decimal monetary values; financial and inventory idempotency; observability and health checks; data isolation; minimization, protection, and retention of personal data; no versioned secrets; limits on code attempts; failure monitoring; backup and recovery before production. An implemented MVP does not authorize real orders: complete operation, availability after inactivity, restoration, security, monitoring, and rollback must be demonstrated within an approved budget. See `docs/DEPLOYMENT_PLAN.md`.

## Open decisions for contracts and operations
F03a and the implementation issues will define the HTTP format, reservation and quote TTLs, selection of two shipping providers, code limits and validity, personal-data retention, email failures, and cancellation/return/refund policy. These details must not weaken the requirement for a complete reservation before payment or authorize a charge without confirmed shipping.

## Metrics
Completed orders; webhook errors; checkout duration; expired reservations and compensations; inventory inconsistencies; quote availability and discrepancies; code delivery; after-sales requests; collection throughput/errors; accuracy of market references; operating cost per service.
