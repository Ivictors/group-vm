# Security and governance

## Principles
Deny by default; least privilege; OIDC authentication through an external provider (initially Keycloak); authorization enforced by services, not only the gateway. Authenticated internal communication. TLS in exposed environments.

## Data
LGPD: data minimization, defined retention, restricted access, and deletion when legally applicable. `customer-service` owns guest contact and address data; `order-service` keeps only the snapshot needed for the order and delivery. Define retention and access for cart, order, and after-sales data before processing real orders. No cart token, access code, or sensitive personal data may appear in a public URL or log.

## Payments
Never store complete card data; use a compatible PSP's hosted checkout/tokenization. Validate the webhook signature against the raw body, timestamp when supported, duplicate events, origin, and allowed transitions. Do not rely on a browser redirect to confirm payment.
Payment starts only after all items have been reserved and the total for items plus shipping has been confirmed. A failed line requires release of partial reservations; an inconclusive response requires reconciliation before a charge is authorized.

## Services
- No shared databases, cross-service table queries, or credentials for another service's database.
- HTTP: timeouts, authentication, validation, payload limits, and retries only when safe.
- Messaging: outbox/inbox, unique event ID, idempotent consumer, DLQ, and auditable replay.
- External URLs: SSRF protection, a per-source allowlist, restrictions on redirects and private addresses, and a maximum response size.
- Logs: correlation/trace ID, without tokens, bank data, secrets, or unnecessary PII.
- The guest cart uses an opaque identifier protected in the browser and expires after seven days without activity; possession of the identifier does not grant access to orders. Order lookup requires a temporary code sent by `notification-service` to the registered email address, with limits on sends and attempts, responses that do not allow order enumeration, and replay protection.
- `shipping-service` restricts destinations to configured CEPs and integrates only authorized/configured providers; it validates external responses, applies timeouts, and does not use an expired price or delivery estimate without buyer reconfirmation.
- Administrative dispatch, tracking, and cancellation/return decisions require authorization and an audit trail. A public request does not automatically issue a refund.
- Keep secrets out of Git; scan for secrets, vulnerable dependencies, and images in CI.
- Test backup and restoration before commercial operation.
- Hosting and costs documented in `docs/DEPLOYMENT_PLAN.md` do not activate production. Before real orders, demonstrate external backup, restoration, availability of the purchase flow, secret isolation, monitoring, and rollback; approve procurement and deployment separately.

## Collection sources
Each connector requires a record of origin, purpose, authorization/license basis, permitted fields, interval, retention, and responsible contact. Do not bypass CAPTCHA, anti-bot measures, paywalls, limits, login, or blocks. Do not enable OLX/Meta scraping without an explicit right to automated access. Prefer authorized APIs, feeds, and partners.

## Vulnerabilities
Report privately to the repository maintainer. Do not put secrets or exploits in the public backlog. During an incident, isolate credentials, record evidence, fix the issue, and validate before reactivating the integration.

## References
OWASP ASVS https://owasp.org/www-project-application-security-verification-standard/
OWASP API Security https://owasp.org/API-Security/
GitHub OIDC https://docs.github.com/en/actions/reference/security/oidc

## Edge, supply chain, and environments
- Kong controls the edge, but each service validates identity and authorization. The absence of Kong/Keycloak in an isolated test does not authorize a production bypass; use stubs only in tests or a separate local configuration.
- Gitleaks, dependency and image analysis, SBOM, and image signing are controls to configure in CI/release through the relevant issues. A configuration file in the repository does not mean a gate has run.
- Pull request CI receives no production secrets. Deployment credentials and permissions are environment-specific and require human approval.
- Public TLS and automated certificates are added when a domain and exposed environment exist; localhost may use a development configuration.
