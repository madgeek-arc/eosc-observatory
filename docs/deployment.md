# EOSC Observatory Backend — Deployment

The executable definitions are [`Dockerfile`](../Dockerfile) and [`Jenkinsfile`](../Jenkinsfile).
This guide summarises them. Production topology (orchestrator, networks) isn't defined in the
repository. The image can run with Docker Compose or on Kubernetes, and the jar can run as a system
service, always as a single instance.

**Run exactly one instance**, including during deployments (no rolling update with two instances
alive). Schedulers, survey-answer locks and the purge lock are in-memory, and the user erasure
procedure assumes a single backend ([user-erasure.md](user-erasure.md)).

## Prerequisites

| Dependency | Notes |
|---|---|
| PostgreSQL | Databases `registry` (also hosts the `commenting`, `external_datasets` and `erasure` schemas by default) and `permissions`. The `tablefunc` extension is needed by the registry |
| Elasticsearch | Reachable at `registry.elasticsearch.uris` (7.x API, registry client 7.17) |
| Redis | Sessions and cache |
| OIDC provider | Client registration for the backend |
| Mailer and messaging services | Reachable at `mailer.client.host` and `messaging-system.client.endpoint`. Set their API keys here if the services require them |
| Statistics tool, Matomo, OpenAI, Google reCAPTCHA, Sentry | Outbound HTTPS |
| Websites referenced in surveys | Outbound HTTP(S) for document extraction |

## Build and package

```bash
./mvnw -B package -DskipTests            # target/observatory-<version>.jar
docker build -t observatory:<version> .  # needs the jar in target/
```

The Docker build has two stages. (`Dockerfile`):

1. `maven:3.9-eclipse-temurin-21-jammy` resolves dependencies and installs Playwright Chromium into
   `/ms-playwright`.
2. `eclipse-temurin:21-jre-jammy` installs Chromium's OS libraries and copies `target/*.jar` and
   `/ms-playwright`. It sets `PLAYWRIGHT_BROWSERS_PATH` and `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1`, and
   runs as the non-root user `observatory` with `java -jar /app/observatory.jar`.

The image declares no `EXPOSE` or `HEALTHCHECK`. Configure the port (`8280`) and probes in the
orchestrator.

## CI/CD (Jenkins)

| Stage | When | What |
|---|---|---|
| Determine Docker Tag | always | Tag = Maven project version |
| Package | always | `./mvnw -B package -DskipTests` |
| Unit tests / Integration tests | non-tag builds | `surefire:test` and `failsafe:integration-test` with JaCoCo. Failures mark the build unstable |
| Dependency Check | always | OWASP with an NVD key from Jenkins credentials |
| MegaLinter | always | `oxsecurity/megalinter-java` (checkstyle `google_checks.xml`, PMD, cspell, jscpd, hadolint, trivy misconfig) |
| Build Image | always | `docker build` tagged `<registry>/observatory:<version>` |
| Upload Image | tags, `main`, `develop` | Push `<version>`. Tags also push `<major.minor>` and `latest`. SNAPSHOTs push `dev` |

Before a release, replace the `SNAPSHOT` dependencies (`authorization`, `messaging-client`,
`mailer-client`) with released versions.
| Handle Releases | `main` | `release-please github-release` and `release-pr` |

The target registry and credentials are defined in the `Jenkinsfile` environment and Jenkins
credentials. They aren't repeated here.

## Runtime

| Aspect | Value |
|---|---|
| Port | `8280`, context path `/api` |
| Volumes | none required. `logs/` is used by the catalogue `/logs` endpoint (log4j2 configuration in `src/main/resources/log4j2.xml`) |
| Start order | PostgreSQL, Elasticsearch, Redis → mailer, messaging-system → backend → frontend |
| Configuration | Environment variables or a mounted properties file (see [configuration.md](configuration.md)) |

## Database migrations

- Registry: Flyway migrations from `registry-starter-service` run at startup. Registry entity
  tables are **validated** (`hbm2ddl.auto=validate`), so they must already exist, for example from a
  restored database. The registry's Flyway migrations only create the Spring Batch tables and the
  `tablefunc` extension and adjust existing sequences; they don't create the entity tables. On an empty
  database, start once with `registry.jpa.properties.hibernate.hbm2ddl.auto=update` (as a command-line
  argument or in `SPRING_APPLICATION_JSON`), then return to `validate`.
- Permissions, commenting, datasets and erasure: Hibernate `update` creates and alters their tables
  and schemas.

## Post-deployment verification

```bash
curl -s http://<host>:8280/api/actuator/health          # status (details only for ADMIN sessions)
curl -s http://<host>:8280/api/v3/api-docs | head -c 200
```

Then log in through the frontend and check that the WebSocket connects (`/api/websocket`).

## Rollback and recovery

Redeploy the previous image tag (`<major.minor>` tags make this easier). Hibernate `update`
changes on the permissions, commenting, datasets and erasure schemas aren't reversible. Restore database
backups if needed. Elasticsearch is rebuilt from PostgreSQL at startup by the registry.
