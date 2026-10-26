# Official technical references

- Spring Boot: https://docs.spring.io/spring-boot/reference/
- Quarkus: https://quarkus.io/guides/
- Angular: https://angular.dev/overview
- Kafka: https://kafka.apache.org/documentation/
- Kong Gateway: https://developer.konghq.com/gateway/
- Keycloak: https://www.keycloak.org/documentation
- PostgreSQL: https://www.postgresql.org/docs/
- Kubernetes: https://kubernetes.io/docs/
- GitHub Actions: https://docs.github.com/en/actions
- GitHub Actions — billing: https://docs.github.com/en/billing/concepts/product-billing/github-actions
- GitHub Actions — environments and review: https://docs.github.com/en/actions/concepts/workflows-and-actions/deployment-environments and https://docs.github.com/en/actions/how-tos/deploy/configure-and-manage-deployments/review-deployments
- Docker Compose — production: https://docs.docker.com/compose/how-tos/production/
- Cloudflare Pages — Angular and pricing: https://developers.cloudflare.com/pages/framework-guides/deploy-an-angular-site/ and https://developers.cloudflare.com/pages/functions/pricing/
- Vercel — plans and commercial use: https://vercel.com/docs/plans/hobby and https://vercel.com/docs/plans/pro-plan/
- Coolify — self-hosted installation and pricing: https://coolify.io/docs/start-with-self-hosted and https://coolify.io/pricing
- Dokploy — installation and hosting options: https://docs.dokploy.com/docs/core/installation and https://docs.dokploy.com/docs/core/differences
- PostgreSQL — continuous backup and restoration: https://www.postgresql.org/docs/current/continuous-archiving.html
- Registro.br — domain pricing: https://registro.br/dominio
- Hetzner — CPU classes and pricing: https://docs.hetzner.com/cloud/servers/faq/ and https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/
- GitHub CLI — editing issues from a file: https://cli.github.com/manual/gh_issue_edit
- GitHub REST API — issue dependencies: https://docs.github.com/en/rest/issues/issue-dependencies
- Gitleaks: https://github.com/gitleaks/gitleaks
- JaCoCo: https://www.jacoco.org/jacoco/trunk/doc/

## Initial implementation version baseline

"Selected" means the version to use when the corresponding service, frontend, check, or deployment is introduced; it does not mean a dependency, image, or cluster is already configured. Recheck patches and compatibility in that implementation issue before pinning a manifest or pulling an image. Local observations come from `java -version`, `mvn -version`, `node --version`, `npm --version`, `ng version`, `docker version`, `docker compose version`, `docker image ls`, `gitleaks version`, `lefthook version`, `kubectl version --client`, and command discovery on PATH. No application containers were started for this audit.

Flags: `LOCAL_VERIFICADO` = a CLI/runtime version was observed; `IMAGEM_EM_CACHE` = the exact selected image tag is cached; `A_BAIXAR_NO_USO` = the exact selected image tag is absent and should be pulled only when needed; `NÃO_ENCONTRADO_NO_PATH` = no CLI was found on PATH, which does not prove it is absent elsewhere; `NÃO_VERIFICADO` = no project dependency/runtime or cluster has been created and validated yet. A flag describes the observation, not implementation readiness.

| Component | Selected baseline | Local observation / flag | Support, compatibility, and follow-up | Official source |
|---|---|---|---|---|
| Java | 21 LTS | Oracle JDK/Javac 21.0.4; `LOCAL_VERIFICADO` | Supported by Spring Boot 4.1.2 and Quarkus 3.40; keep Java 21 for the first services. | [Spring Boot requirements](https://docs.spring.io/spring-boot/system-requirements.html), [Quarkus prerequisites](https://quarkus.io/version/3.40/guides/getting-started/) |
| Maven Wrapper | 3.9.16 per service | Global Maven 3.9.12; `LOCAL_VERIFICADO`; no service wrapper yet, `NÃO_VERIFICADO` | 3.9.16 is a maintained Maven 3 release and is the version named in the Quarkus 3.40 guide. Add a wrapper in each service's bootstrap issue; do not add another global installation. | [Maven release history](https://maven.apache.org/docs/history.html), [Quarkus prerequisites](https://quarkus.io/version/3.40/guides/getting-started/) |
| Spring Boot | 4.1.2 | No service dependency yet; `NÃO_VERIFICADO` | Requires Java 17+ and Maven 3.6.3+; Java 21 and Maven Wrapper 3.9.16 meet these requirements. | [Spring Boot requirements](https://docs.spring.io/spring-boot/system-requirements.html) |
| Quarkus | 3.40.1.1 (3.40 LTS) | No service dependency yet; `NÃO_VERIFICADO` | Latest production-recommended LTS line; community maintenance through 2027-09-30. Guide requires JDK 17+ and Maven 3.9.16. | [Release/support table](https://quarkus.io/releases/), [getting started](https://quarkus.io/version/3.40/guides/getting-started/) |
| Node.js / npm | Node 24.22.1 LTS / npm 11.16.0 initial local baseline | Node 24.18.0 / npm 11.16.0; `LOCAL_VERIFICADO` | Existing Node meets Angular 22's `^24.15.0` range; update the 24.x installation in place to the newer security patch before frontend bootstrap. Verify the bundled npm version after that update. | [Node 24.22.1 release](https://nodejs.org/en/blog/release/v24.22.1), [Angular compatibility](https://angular.dev/reference/versions) |
| Angular / CLI / TypeScript | Angular core 22.2.3, project CLI 22.2.4, TypeScript 6.0.3 | Global CLI 21.2.11; `LOCAL_VERIFICADO`; no frontend dependency yet, `NÃO_VERIFICADO` | Angular 22 is in active support and requires Node `^22.22.3 || ^24.15.0 || ^26.0.0` and TypeScript `>=6.0.0 <6.1.0`. Install CLI and TypeScript as project dependencies during frontend bootstrap; do not use global CLI 21 to choose the project major. | [Angular releases](https://angular.dev/reference/releases), [compatibility](https://angular.dev/reference/versions), [core package](https://www.npmjs.com/package/@angular/core), [CLI package](https://www.npmjs.com/package/@angular/cli), [TypeScript package](https://www.npmjs.com/package/typescript) |
| Docker Engine / Compose | Local development baseline: Engine 29.1.3 / Compose 2.40.3-desktop.1 | CLI 29.1.3, Engine 29.1.3, Compose 2.40.3-desktop.1; `LOCAL_VERIFICADO` | Daemon was reachable on this audit. Docker Desktop manages the installed Engine/Compose pair; review its updates in place before a Docker-dependent implementation. These versions do not pin a production host. | [Engine 29 release notes](https://docs.docker.com/engine/release-notes/29/), [Compose releases](https://github.com/docker/compose/releases) |
| PostgreSQL | 18.6; `postgres:18.6` | `postgres:18` cached, exact `postgres:18.6` absent; CLI not on PATH; `A_BAIXAR_NO_USO`, `NÃO_ENCONTRADO_NO_PATH` | 18.6 is the current supported 18.x patch. A mutable `postgres:18` cache tag does not establish its patch version. Pull the exact tag when a service needs a database. | [Version policy](https://www.postgresql.org/support/versioning/), [18.6 release](https://www.postgresql.org/docs/18/release-18-6.html), [official image](https://hub.docker.com/_/postgres) |
| Kafka KRaft | 4.3.1; `apache/kafka:4.3.1` | Exact image cached; CLI not on PATH; `IMAGEM_EM_CACHE`, `NÃO_ENCONTRADO_NO_PATH` | Supported Apache release with an official image. Cached is not running or integration-tested; start only for the relevant issue. | [Apache Kafka downloads](https://kafka.apache.org/community/downloads/) |
| Kong Gateway Community/OSS | 3.9.4; `kong:3.9.4` | Exact image absent; CLI not on PATH; `A_BAIXAR_NO_USO`, `NÃO_ENCONTRADO_NO_PATH` | Community/OSS chosen for the current budget. Release and official image exist, but Kong's published support schedule explicitly lists Enterprise editions and must not be applied to OSS. Before exposing production traffic, confirm OSS security maintenance and gateway requirements in its implementation issue. | [OSS release](https://github.com/Kong/kong/releases/tag/3.9.4), [official image tags](https://hub.docker.com/_/kong/tags), [Enterprise support policy](https://developer.konghq.com/gateway/version-support-policy/) |
| Keycloak | 26.8.1; `quay.io/keycloak/keycloak:26.8.1` | Exact image absent; CLI not on PATH; `A_BAIXAR_NO_USO`, `NÃO_ENCONTRADO_NO_PATH` | Current security-fix release; pull and validate configuration when identity integration is implemented. | [26.8.1 release](https://www.keycloak.org/2026/10/keycloak-2681-released), [container guide](https://www.keycloak.org/server/containers) |
| Kubernetes / kubectl | Target cluster 1.37.1 / matching kubectl 1.37.1 | kubectl client 1.34.1; `LOCAL_VERIFICADO`; no target cluster, `NÃO_VERIFICADO` | 1.37 is supported. kubectl 1.34 is outside the supported one-minor skew for a 1.37 API server; update kubectl in place only if a 1.37 cluster is selected. Local development does not require a cluster. | [Kubernetes releases](https://kubernetes.io/releases/), [kubectl skew policy](https://kubernetes.io/docs/concepts/overview/kubectl/) |
| Helm | 4.3.1 for the Kubernetes target | No CLI on PATH; `NÃO_ENCONTRADO_NO_PATH` | Introduce only with deployment charts and verify compatibility with the selected cluster then. | [Helm release](https://github.com/helm/helm/releases/tag/v4.3.1), [Helm version skew](https://blog.helm.sh/docs/topics/version_skew/) |
| Gitleaks | 8.30.1 | 8.30.1; `LOCAL_VERIFICADO` | Local scanner matches the selected release; repository/CI enforcement remains a separate implementation task. | [Gitleaks release](https://github.com/gitleaks/gitleaks/releases/tag/v8.30.1) |
| JaCoCo | 0.8.16 Maven plugin | No service POM or plugin yet; `NÃO_VERIFICADO` | Configure per service or CI in a later implementation issue; no global CLI installation is needed. | [JaCoCo release](https://github.com/jacoco/jacoco/releases/tag/v0.8.16) |
| Lefthook | 2.1.10 local baseline | 2.1.10; `LOCAL_VERIFICADO` | Existing local hook tool can be reused; review an in-place update before relying on it as a required gate. | [Lefthook releases](https://github.com/evilmartians/lefthook/releases) |

The matrix is a planning baseline, not evidence of a built application or a production-ready deployment. Exact image tags that are absent are intentionally left for pull-on-use. Framework dependencies, wrappers, frontend packages, checks, and deployment tooling will be configured in their respective approved issues.
