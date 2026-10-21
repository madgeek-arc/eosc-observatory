# EOSC Observatory Backend

Core backend service of the EOSC Observatory. It manages surveys and survey answers, the
stakeholder/coordinator/administrator organisation model, user accounts and permissions, in-survey
collaboration (comments and messaging), indicators, news, and AI-assisted extraction of documents
referenced in survey answers.

## Component metadata

| Field | Value |
|---|---|
| Component | EOSC Observatory Backend |
| Type | Backend service |
| Status | Released (see [CHANGELOG](CHANGELOG.md)) |
| Technology | Java 21 / Spring Boot 3.5 / Maven |
| API | Yes |
| Swagger | `/api/swagger-ui/index.html` on a running instance (spec at `/api/v3/api-docs`) |
| Deployment | Docker |
| Documentation | [docs/](docs/) |

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

| Component | Relationship |
|---|---|
| `registry` (`registry-starter-service`, `registry-starter-elasticsearch`, `registry-core-helper`) | Embedded library |
| `catalogue` | Embedded library |
| `authorization` | Embedded library |
| `messaging-client`, `mailer-client` | Clients of the messaging system and mailer services |
| `document-analyzer` | Embedded library (OpenAI-based) |
| `recaptcha`, `request-logging-starter` | Embedded libraries |

Several of these are `SNAPSHOT` artefacts resolved from `https://repo.madgik.di.uoa.gr`.

## Prerequisites

- JDK 21
- PostgreSQL with the databases `registry` and `permissions` (the `registry` database is also used by the
  `commenting` and `external_datasets` schemas)
- Elasticsearch (host `${fqdn}:9200` by default)
- Redis (`localhost:6379` by default)
- An OIDC provider registered through `spring.security.oauth2.client.*`
- Access to the Madgik Maven repository for the library dependencies

## Quick start

`src/main/resources/application.properties` ships with empty credentials and a placeholder OpenAI key.
Provide a local override (for example `application-local.properties`, or environment variables) with at
least the database, Elasticsearch, Redis and OIDC settings, `mailer.from`, `observatory.*` redirect URLs and
`analytics.url`/`analytics.token` (the latter two have no defaults and are required at startup), then:

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

## Documentation

- [Architecture](docs/architecture.md)
- [Integration](docs/integration.md)
- API: Swagger UI at `/api/swagger-ui/index.html`, with groups *EOSC Observatory*, *Dynamic Catalogue*
  and *Registry Core*. The OpenAPI document is generated at runtime from the controllers, so there is
  no version-controlled copy.
- Deployment: [Dockerfile](Dockerfile) and [Jenkinsfile](Jenkinsfile)
- [Changelog](CHANGELOG.md)

## Licence

Apache License 2.0, see [LICENSE](LICENSE).
