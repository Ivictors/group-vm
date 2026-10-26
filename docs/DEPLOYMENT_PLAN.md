# Deployment and cost planning — VmGriffes

## Constraint and comparison method

The current ceiling is **R$ 100 per month for the entire operation**, including the server, prorated .br domain, external backup, email, and observability. Do not buy only a server that fits the ceiling while leaving recovery or critical components uncovered. A single machine with tested restoration is the initial tolerated setup; its failure may interrupt sales. High availability has neither been approved nor funded.

Every production proposal must state: region and data residency, quote currency and exchange rate, taxes, shared or dedicated CPU, RAM, disk and I/O, IPv4, traffic/egress, snapshots and external backup, email, domain, monitoring, limits and variable costs, and the single maintainer's operational work. Record the total monthly and annual price, date, and source link. Measure actual processes before sizing; the platform vendor's minimums do not represent the needs of the ten MVP services and infrastructure.

## Angular frontend

| Option | Application to the project | Costs and limits to check before choosing |
|---|---|---|
| [Cloudflare Pages](https://developers.cloudflare.com/pages/framework-guides/deploy-an-angular-site/) | Initial candidate for static Angular files. Supports a monorepo and custom domain; validate the build, routes, and secure API integration when the frontend exists. | Static requests are free; Functions use the Workers quota. Check current build, file, transfer, and commercial-policy limits. [Pricing](https://developers.cloudflare.com/pages/functions/pricing/), [limits](https://developers.cloudflare.com/pages/platform/limits/), [monorepo](https://developers.cloudflare.com/pages/configuration/monorepos/). |
| [Vercel](https://vercel.com/docs/plans) | Alternative for the same Angular build; compare previews, domain, observability, and monorepo integration. | Hobby is restricted to personal/noncommercial use; obtain a quote for Pro or an applicable commercial plan for the store. Pro advertises a US$ 20/month base fee before taxes and additional usage. [Hobby](https://vercel.com/docs/plans/hobby), [Pro](https://vercel.com/docs/plans/pro-plan/). |

An available static frontend does not prove checkout availability: the API, database, reservations, shipping, email, and payment must also respond. No frontend provider has been selected or configured. Automatic publishing from `main` may be enabled only after the production approval flow is defined; a simple Git integration may publish every push.

## Services and execution platform

| Option | Advantage | Operational cost/limit to measure |
|---|---|---|
| [Docker Compose on a VPS](https://docs.docker.com/compose/how-tos/production/) | Smaller control layer and clearer resource use; allows always-on processes on one machine. | The maintainer manages Docker, TLS, updates, logs, volumes, deployment, and recovery. One machine is a single point of failure. |
| [Self-hosted Coolify on a VPS](https://coolify.io/docs/start-with-self-hosted) | Application dashboard and automation on owned infrastructure. | The dashboard officially requires at least 2 CPUs, 2 GB RAM, and 10 GB free for installation; services need additional resources. Separate builds from the customer-serving machine when measurements require it. |
| [Self-hosted Dokploy on a VPS](https://docs.dokploy.com/docs/core/installation) | Alternative dashboard and automation. | Installation recommends at least 2 GB RAM and 30 GB disk; services and builds require additional resources. Check coexistence with Kong, TLS, and the private network. |
| Managed containers, for example [Cloud Run](https://cloud.google.com/run/docs/about-instance-autoscaling) | Less host maintenance; usage-based pricing may favor sparse workloads. | Scaling to zero causes cold starts; Kafka tasks and background processing need a design and continuous capacity. Paid minimum instances, database, broker, gateway, identity, network, and egress all count toward cost. |
| Kubernetes/Helm | Already approved target architecture; preserves a future deployment path. | Size the cluster, operations, and cost when a budget is available. Do not assume the target fits the current ceiling or change service architecture to save money. |

**VPS type:** compare shared CPU, dedicated CPU in a virtual machine, and a dedicated physical server as distinct classes. [Hetzner distinguishes shared plans from CCX plans with dedicated CPU](https://docs.hetzner.com/cloud/servers/faq/); price alone does not measure required memory. In the official table consulted on 2026-10-22, CCX13 in Germany/Finland was listed at **€ 42.99/month, excluding VAT and IPv4**. This is only an example of dedicated-CPU pricing, not approved sizing for the project. [Price table](https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/). Also quote providers with a Brazilian region and calculate latency, taxes, and international data transfer before choosing.

| Scenario to quote | Costs published when consulted | Project costs still unpriced |
|---|---|---|
| Pages + shared-CPU VPS + Compose | Static Pages: free requests; .br domain: R$ 40/year. | VPS plan and region with measured RAM, IPv4, external backup, email, monitoring, taxes, and exchange rate. |
| Pages + dedicated-CPU VPS + Coolify or Dokploy | Self-hosted Coolify and Dokploy have no license fee; CCX13 example: € 42.99/month before VAT/IPv4; .br domain: R$ 40/year. [Coolify](https://coolify.io/pricing), [Dokploy](https://docs.dokploy.com/docs/core/differences). | Additional RAM for dashboard and services, disk, backup, email, monitoring, network, taxes, and exchange rate. CCX13 has not been validated for this workload. |
| Vercel Pro + managed containers/data | Vercel Pro: US$ 20/month base fee; .br domain: R$ 40/year. | Minimum instances, PostgreSQL, Kafka, Keycloak, Kong, storage, traffic, backup, email, monitoring, taxes, and exchange rate. |

No row is a total price or purchase proposal. Do not add amounts in different currencies without a current exchange rate; record the source and rate used once a configuration has been measured. If a required component has no price, the comparison has not shown that the setup fits the ceiling.

For the purchase path, measure catalog, cart, customer, shipping, order, inventory, payment, notification, Kong, Keycloak, PostgreSQL, and Kafka running together. Intelligence remains in the MVP; measure the collector and intelligence within the same budget even if collection jobs can run on their own schedule. Do not assume a component needed for the first purchase, webhook, reservation expiration, or code delivery can be suspended without proving startup, processing, and recovery within the required time.

## Data, domain, and operations

- **PostgreSQL:** keep separate databases and credentials per service, even when several databases run on one physical server. Compare self-hosted and managed instances based on RAM, connections, disk, IOPS, updates, external backup, and restoration. Define RPO/RTO from commercial operations before purchase; test database and WAL restoration or another strategy compatible with the chosen version. A snapshot on the same VPS is not a copy outside the failure domain. [PostgreSQL: PITR](https://www.postgresql.org/docs/current/continuous-archiving.html).
- **Kafka KRaft, Keycloak, and Kong:** include broker storage/CPU, identity, and edge costs; check startup, persistence, updates, secrets, and recovery. Do not replace the approved technologies in this study.
- **.br domain:** [Registro.br listed R$ 40/year](https://registro.br/dominio) when consulted on 2026-10-22, approximately R$ 3.33/month for comparison only. The name, availability, DNS, TLS, and renewal have not been purchased or configured; obtain fresh quotes if costs change.
- **Email and observability:** quote transactional sends and limits, external availability monitoring, metrics, logs, and alerts without personal data. Include storage/retention, traffic, and alert delivery. With one machine, external monitoring must remain available when that machine fails.
- **Security and recovery:** restricted admin access, secrets outside Git, HTTPS, updates, encrypted backups outside the VPS, a documented restoration procedure, and immutable-image rollback. Restoration must preserve consistency across orders, payments, inventory, and events; proof of backup is not proof of restoration.

## GitHub Actions and future delivery

Current CI checks only foundation files; `.github/workflows/release.yml` intentionally fails and publishes nothing. Plan checks per changed service, required integration tests, Maven/npm caches, concurrency control, short artifact retention, and immutable images, with reports that distinguish jobs that ran from jobs that were skipped. Publish images only after applicable checks. Production deployment will be a separate, manually authorized step with health verification and rollback. This document implements none of those steps.

## Gate before real orders

1. Measure the complete Commerce and Intelligence MVP within one operating budget. Record actual resource use and availability after inactivity, including startup, purchase, webhook, reservation expiration, and email code delivery.
2. Demonstrate the buyer and operator flows with the required services: confirmed shipping, complete reservation before payment, authenticated webhook, idempotent order and inventory changes, failure compensation and reconciliation, order lookup, fulfillment, and after-sales requests. Define cancellation, return, refund, and personal-data retention policies before real orders.
3. Demonstrate an external, encrypted backup and a tested restoration that preserves consistency across orders, payments, inventory, and events. Record achieved recovery objectives and a tested rollback procedure.
4. Verify authorization, secret isolation, HTTPS, monitoring and alerts, and operational response to failures. Record the evidence and remaining limitations; an available frontend or passing CI alone does not prove that the purchase flow is available.
5. Confirm a complete monthly and annual cost, including infrastructure, domain, external backup, email, monitoring, taxes, exchange rates, and variable usage, within the approved budget. Refresh dated prices and terms before procurement.
6. Obtain separate human approvals for procurement, configuration, and deployment when each action occurs. After the evidence above has been reviewed, obtain explicit human approval to accept real orders. Implementing the MVP, passing CI, or having this plan alone does not authorize real sales.

Review prices, plans, terms, and versions in official sources before procurement or configuration. Values on this page are dated examples, not a finalized budget.
