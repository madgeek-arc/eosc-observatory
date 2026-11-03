# EOSC Observatory Backend — Development

## Tools

| Tool | Version | Evidence |
|---|---|---|
| JDK | 21 | `pom.xml`, `Dockerfile` |
| Maven | wrapper (`./mvnw`) | `.mvn/wrapper` |
| Docker | for Testcontainers integration tests and image builds | `pom.xml` (Testcontainers), `Dockerfile` |
| Local services | PostgreSQL, Elasticsearch 7.x, Redis, an OIDC provider (for example Keycloak) | `application.properties` |
| Playwright Chromium | for document extraction outside Docker | `Dockerfile` |

## Commands

```bash
./mvnw -B package -DskipTests                       # build target/observatory-<version>.jar
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments=--spring.config.additional-location=file:./application-local.properties
./mvnw -B test                                      # unit tests (surefire)
./mvnw -B verify                                    # + integration tests (failsafe, needs Docker)
./mvnw -B dependency-check:check -DnvdApiKey=<key>  # OWASP scan
```

API at `http://localhost:8280/api`. Swagger UI at `/api/swagger-ui/index.html`.

## Local setup notes

- Start from [`application-example.properties`](../application-example.properties). Keep the default
  `mail-debug` profile so no real email is sent. Set `observatory.invitationKey` (startup fails
  without it) and `observatory.hmacSecret` (user purge fails without it).
- The registry schema is validated at startup. Point `registry.datasource.*` at a database that
  already has the registry tables, or temporarily set `registry.jpa.properties.hibernate.hbm2ddl.auto=update`
  locally.
- The embedded libraries (registry, catalogue, authorization, document-analyzer, clients) are
  resolved from the madgik Maven repository. After changing one locally, `mvn install` it and rebuild
  this project. `messaging-client` `0.1.1-SNAPSHOT` must be installed from the messaging-system
  repository until it's published.
- Run one instance: locks and schedulers are in-memory.
- Install Playwright Chromium once for local runs:
  `./mvnw exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps chromium"`.

## Code generation and linting

- MapStruct mappers are generated at compile time (`mappers` package).
- MegaLinter (CI) runs checkstyle with `google_checks.xml`, PMD (`.megalinter/pmd-rules.xml`), cspell,
  jscpd, hadolint and trivy on `src/`, `Dockerfile`, `Jenkinsfile` and `pom.xml`. To run it locally:
  `docker run --rm -v "$PWD":/tmp/lint oxsecurity/megalinter-java:latest`.

## Debugging

- WebSocket noise from stale CSRF tokens is muted (`ExceptionWebSocketHandlerDecorator=ERROR`).
- `MailDebugConfig` keeps sent emails in memory and logs them, with masked addresses, when
  `mail-debug` is active.
