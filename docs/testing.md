# EOSC Observatory Backend — Testing

## Suites

| Type | Runner | Classes | Dependencies |
|---|---|---|---|
| Unit | surefire (`./mvnw test`, excludes `**/*Tests.java`) | 42 `*Test` classes under `src/test/java`: services, erasure, mail delivery, security rules and expressions, messaging authorisation, CORS, dashboards, invitations, privacy advice, statistics access, utils, DTOs | none (Mockito, Spring MockMvc, HSQLDB, in-JVM HTTP stubs) |
| Integration | failsafe (`**/*Tests.java`: `ObservatoryApplicationTests`, `CommentingTests`), with `IntegrationTestConfig` | Spring context with Testcontainers PostgreSQL and Elasticsearch | Docker |

`ErasureRecordPostgresTest` runs only against a real PostgreSQL given with
`-Dregistry.datasource.url=...` and is skipped otherwise.

Test configuration: `src/test/resources/application.properties` (dummy secrets, HSQLDB authorization
and erasure datasources, Flyway on) and `log4j2-test.xml`.

Security-relevant suites:

| Class | Covers |
|---|---|
| `SecurityConfigUrlRulesTest` | `ADMIN` on registry `/version/**`, `/schemaService/**`, `/resourcesync/**`; login for `/request/default`; public paths stay public |
| `SimpleCORSFilterTest` | CORS headers only for configured origins |
| `MessagingSystemControllerTest` | Participant check on read receipts, sender check on new threads, `ADMIN` on thread update/delete (method security against a stub messaging service) |
| `SecurityConfigTest` | `ADMIN` mapping, no admins when unset, CSRF cookie |
| `MailerConfigTest`, `ResilientMailerTest` | Mailer API key header, retry classification (no retry on read timeout, `4xx`, `502`) |

## Results on `fix/report-issues`

`./mvnw -B verify -Ddependency-check.skip=true`: **299 unit tests** (1 skipped:
`ErasureRecordPostgresTest`), **9 integration tests**, checkstyle passes. `develop` @ `0f4c52d` has
281 unit tests.

## Coverage and CI

Jenkins runs unit and integration tests separately with JaCoCo agents (`jacoco:prepare-agent`,
`prepare-agent-integration`), publishes JUnit reports, records coverage, and marks the build
*unstable* on failures. MegaLinter and OWASP dependency-check run in parallel.
(`Jenkinsfile`)

## Commands

```bash
./mvnw -B test
./mvnw -B test -Dtest=StatsQuerySecurityTest
./mvnw -B verify          # includes failsafe integration tests (Docker required)
./mvnw -B jacoco:report   # after tests with the JaCoCo agent
```
