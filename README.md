# EOSC Observatory Backend

Core backend service of the EOSC Observatory. It manages surveys and survey answers, the
stakeholder/coordinator/administrator organisation model, user accounts and permissions, in-survey
collaboration (comments and messaging), dashboards, news, GDPR user erasure, and AI-assisted
extraction of documents referenced in survey answers.

## Role within the EOSC Observatory

The service is the single backend consumed by the Observatory web frontend. It embeds the
`registry` (typed-resource persistence on PostgreSQL + Elasticsearch), `catalogue` (dynamic survey
models and forms) and `authorization` (resource permissions) libraries, and integrates with an OIDC
identity provider, a mailer, a messaging system, a statistics tool, Matomo analytics and an OpenAI
model. See [docs/architecture.md](docs/architecture.md) and [docs/integration.md](docs/integration.md).

## Technology

- Java 21, Spring Boot 3.5 (Web, Security, OAuth2 Client, Data JPA, WebSocket/STOMP, Actuator, AOP)
- Maven (wrapper included), MapStruct
- PostgreSQL, Elasticsearch, Redis (sessions and survey-answer cache)
- FreeMarker (email templates), springdoc-openapi (Swagger UI)
- Sentry, Log4j2

## Dependencies on other components

| Component | Version | Relationship |
|---|---|---|
| `registry` (`registry-starter-service`, `registry-starter-elasticsearch`, `registry-core-helper`) | 4.3.1 | Embedded library |
| `catalogue` | 9.1.0 | Embedded library |
| `authorization` | 2.1.0-SNAPSHOT | Embedded library |
| `messaging-client` | 0.1.1-SNAPSHOT | Client of the messaging system service |
| `mailer-client` | 0.0.2-SNAPSHOT | Mailer API types. Mail is sent by the application's own HTTP client (`ResilientMailer`) |
| `document-analyzer` | 1.1.0 | Embedded library (OpenAI-based; launches headless Chromium) |
| `recaptcha`, `request-logging-starter` | 1.0.0 each | Embedded libraries |

`authorization`, `messaging-client` and `mailer-client` are `SNAPSHOT` artefacts resolved from
`https://repo.madgik.di.uoa.gr`. Release them before releasing this application, or its builds
can't be reproduced.

## Prerequisites

- JDK 21
- PostgreSQL with the databases `registry` and `permissions` (the `registry` database also hosts the
  `commenting`, `external_datasets` and `erasure` schemas)
- Elasticsearch (host `${fqdn}:9200` by default)
- Redis (`localhost:6379` by default)
- An OIDC provider registered through `spring.security.oauth2.client.*`
- Access to the Madgik Maven repository for the library dependencies

## Quick start

`src/main/resources/application.properties` ships with empty credentials and a placeholder OpenAI key.
Provide a local override (for example `application-local.properties`, or environment variables) with at
least the database, Elasticsearch, Redis and OIDC settings, `mailer.from`, `observatory.*` redirect URLs and
`analytics.url`/`analytics.token` (no defaults, required at startup), `observatory.invitationKey`
(a base64 256-bit key, required at startup) and `observatory.hmacSecret` (required for user erasure).
See [docs/configuration.md](docs/configuration.md). Then:

```bash
./mvnw spring-boot:run
```

The API is served at `http://localhost:8280/api`. Note that `spring.profiles.active=mail-debug` is the
default, so outgoing emails are logged instead of sent.

To build the runnable jar:

```bash
./mvnw -B package -DskipTests
java -jar target/observatory-*.jar
```

Deployed environments run a Docker image built and pushed by Jenkins ([Dockerfile](Dockerfile),
[Jenkinsfile](Jenkinsfile)), as a single instance. See [docs/deployment.md](docs/deployment.md).

## Documentation

- [Architecture](docs/architecture.md) · [Integration](docs/integration.md) · [API](docs/api.md)
- [Configuration](docs/configuration.md) · [Example configuration](application-example.properties)
- [Deployment](docs/deployment.md) · [Operations](docs/operations.md) · [Troubleshooting](docs/troubleshooting.md)
- [Security model](docs/security-model.md) · [Security policy](SECURITY.md) · [User erasure](docs/user-erasure.md) · [Development](docs/development.md) · [Testing](docs/testing.md)
- API: Swagger UI at `/api/swagger-ui/index.html`, with groups *EOSC Observatory*, *Dynamic Catalogue*
  and *Registry Core*. The OpenAPI document is generated at runtime from the controllers, so there is
  no version-controlled copy.
- [Contributing](CONTRIBUTING.md) · [Changelog](CHANGELOG.md)

## Licence

Apache License 2.0, see [LICENSE](LICENSE). Maintained by the madgeek-arc team
(<https://github.com/madgeek-arc/eosc-observatory>).
