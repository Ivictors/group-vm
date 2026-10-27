# Technical debt and known gaps

- [x] Demonstrate effective protection of `main`; configure the ruleset, area labels, and milestones. Completed in issue #5 and PR #6.
- [ ] Provision PostgreSQL with separate databases/credentials per service; introduce Kafka KRaft and Keycloak for integrations that require them.
  - [x] Document a valid local PostgreSQL 18.6 Compose example without provisioning credentials or a service database (issue #7).
  - [ ] Provision local PostgreSQL and verify a separate catalog database, role, and persistence (issue #8).
  - [ ] Add separate databases and credentials as later services are implemented.
  - [ ] Introduce Kafka KRaft when an approved event integration requires it (issue #11).
  - [ ] Introduce Keycloak when an approved identity integration requires it (issue #12).
- [ ] Create the first independent service with a Maven Wrapper, build, tests, and OCI image (issue #9).
- [ ] Configure unit, integration, contract, and E2E tests, with Testcontainers when needed (service CI starts in issue #10).
- [ ] Add linting, formatting, static analysis, coverage, and dependency, secret, and image scans.
- [ ] Version OpenAPI contracts and event schemas, with compatibility tests.
- [ ] Implement outbox/inbox, DLQ, replay, reconciliation, and distributed observability as the flows are built.
- [ ] Validate concrete versions and compatibility for Java, Spring Boot, Quarkus, Angular, PostgreSQL, Kafka, Kong, and Keycloak.
- [ ] Select and document the sandbox PSP and its signature/idempotency rules.
- [ ] Select two shipping providers and a test email provider; define quote/code validity, fallback, and abuse limits in F03a/C10–C12.
- [ ] Define operational cancellation, return, and refund policy before accepting real orders; a guest request does not automatically issue a refund.
- [ ] Define backup, restoration, retention, and operational policies before production.
- [ ] Fix/validate UTF-8 encoding in Windows tools to prevent non-ASCII documentation text from appearing corrupted.

No preexisting test failures are recorded: the application has no code or executable test harness yet.
