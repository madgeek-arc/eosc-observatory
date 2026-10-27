# EOSC Observatory Backend — Configuration

Defaults ship in `src/main/resources/application.properties`, packaged in the jar and image.
Override them with environment variables (Spring Boot relaxed binding, for example
`REGISTRY_DATASOURCE_PASSWORD`), `--spring.config.additional-location`, or an
`application-<profile>.properties` file. Standard Spring Boot precedence applies. A copyable,
placeholder-only example is in [`../application-example.properties`](../application-example.properties).
Secrets are never shown here.

Keys and defaults come from `application.properties` and the `@ConfigurationProperties`/`@Value`
sources listed. "Required" marks keys without which startup or
a core feature fails.

## Server and Spring

| Property | Required | Default | Description |
|---|---|---|---|
| `server.port` | No | `8280` | HTTP port |
| `server.servlet.context-path` | No | `/api` | All endpoints are under it |
| `server.servlet.session.timeout` | No | `120m` | Session lifetime (sessions stored in Redis) |
| `server.servlet.session.cookie.domain` | No | empty | Set when the frontend runs on a sibling domain |
| `spring.profiles.active` | No | `mail-debug` | `mail-debug` replaces the mailer with a logging stub (`configuration/MailDebugConfig.java`). **Override it in deployed environments to send real email** |
| `spring.servlet.multipart.max-file-size` / `max-request-size` | No | `100MB` | Upload limits (CSV, datasets, restore) |
| `management.endpoint.health.show-details` / `.roles` | No | `when-authorized` / `ADMIN` | Health component details only for `ADMIN` users |

## Application (`observatory.*`, `configuration/ApplicationProperties.java`)

| Property | Required | Default | Description |
|---|---|---|---|
| `observatory.admins` | **Yes** | none (not in the shipped file) | Emails granted the `ADMIN` authority at login (`SecurityConfig`). Unset means nobody is `ADMIN` |
| `observatory.login-redirect` | **Yes** | — | Frontend URL after login |
| `observatory.logout-redirect` | **Yes** | — | Frontend URL after logout |
| `observatory.base-url` | Yes, for comment notifications | — | Public frontend base URL used in notification links (`commenting/CommentNotificationService`) |
| `observatory.invitationKey` | **Yes** | `${OBSERVATORY_INVITATION_KEY:}` | Base64 256-bit key that encrypts invitation tokens (`openssl rand -base64 32`). Startup fails if it's missing or not 32 bytes (`InvitationServiceImpl`). Changing it invalidates outstanding invitations. **Secret** |
| `observatory.invitationTtl` | No | `7d` | Invitation validity |
| `observatory.hmacSecret` | **Yes**, for user erasure | `${OBSERVATORY_HMAC_SECRET:}` | Keys the pseudonymous subject reference in the erasure register and log pseudonyms. Without it, purges fail and log masking is disabled (startup warning). Keep it stable and backed up: changing it makes past references unmatchable. **Secret** |
| `observatory.corsAllowedOrigins` | No | empty | Origins that get credentialed CORS headers (`SimpleCORSFilter`). Leave empty when the frontend is served from the same origin |
| `observatory.surveySchedulerCron` | No | `-` (disabled) | Cron for survey start/end/deadline emails (for example `0 0 10 * * *`) |

## Identity provider

| Property | Required | Default | Description |
|---|---|---|---|
| `spring.security.oauth2.client.registration.<id>.*` / `.provider.<id>.*` | **Yes** | — (not shipped) | OIDC client registration (client ID and secret, scopes including `email`, issuer). **Secret** (client secret) |

## Persistence

| Property | Required | Default | Description |
|---|---|---|---|
| `fqdn` | No | `localhost` | Host used to build the default DB, Elasticsearch and `registry.host` URLs |
| `registry.datasource.url` / `.username` / `.password` / `.driver-class-name` | **Yes** | `jdbc:postgresql://${fqdn}:5432/registry`, empty credentials | Registry database. **Secret** (password) |
| `registry.jpa.properties.hibernate.hbm2ddl.auto` | No | `validate` | The schema must already exist (created by registry Flyway migrations and Hibernate) |
| `registry.jpa.properties.hibernate.enable_lazy_load_no_trans` | No | `true` | Needed by the registry dump |
| `registry.host` | **Yes** (registry) | `http://${fqdn}:${server.port}/${server.servlet.context-path}` | Public base URL used by registry ResourceSync |
| `registry.resource-type-init.location` | No | `classpath:resourceTypes` | Resource types created at startup |
| `authorization.datasource.*`, `authorization.jpa.properties.*` | **Yes** | `.../permissions`, `hbm2ddl.auto=update` | Permission store (authorization library) |
| `commenting.datasource.*`, `commenting.jpa.properties.*` | **Yes** | `.../registry`, schema `commenting`, `update` | Comment threads (`commenting/CommentingDatasourceConfig.java`) |
| `datasets.datasource.*`, `datasets.jpa.properties.*` | **Yes** | `.../registry`, schema `external_datasets`, `update` | External datasets (`datasets/DatasourceConfig.java`) |
| `erasure.datasource.*`, `erasure.jpa.properties.*` | **Yes** | `.../registry`, schema `erasure`, `update` | Erasure register (`erasure/ErasureDatasourceConfig.java`) |
| `*.datasource.configuration.*` | No | Hikari defaults | Pool settings per datasource |
| `registry.elasticsearch.uris` / `.username` / `.password` | **Yes** | `${fqdn}:9200`, empty | Elasticsearch. **Secret** (password) |
| `elastic.aggregation.topHitsSize` / `bucketSize`, `elastic.index.max_result_window` | No | `100` / `100` / `10000` | Search tuning |
| `spring.data.redis.host` / `.port` / `.password` | **Yes** | `localhost` / `6379` / shipped value to be overridden | Sessions and cache. **Secret** (password) |

## Integrations

| Property | Required | Default | Description |
|---|---|---|---|
| `mailer.client.host` | **Yes** (when not using `mail-debug`) | empty | Mailer base URL, including its context path. Empty logs a warning and every mail fails |
| `mailer.client.api-key` | When the mailer sets `mail.api-key` | unset | Sent as `X-API-Key` (`MailerConfig`). **Secret** |
| `mailer.client.connect-timeout` / `.read-timeout` | No | `3s` / `30s` | Mailer HTTP timeouts |
| `mailer.retry.max-attempts` / `.delay` | No | `5` / `1s` | Retries for connection failures and `5xx` other than `502` |
| `mailer.from` | **Yes** | empty | Sender address. The key is shipped empty. If the key is removed entirely, startup fails (`@Value` without a default) |
| `messaging-system.client.endpoint` | **Yes** | empty | Messaging service base URL (for example `http://<host>:8383/api/`) |
| `messaging-system.client.api-key` | When the messaging service sets `messaging-system.api-key` | unset | Sent as `X-API-Key`. **Secret** |
| `messaging-system.client.connect-timeout` / `.response-timeout` | No | `10s` / `30s` | Messaging HTTP timeouts |
| `stats-tool.endpoint` | **Yes** | empty | Statistics tool base URL. Shipped empty, which makes statistics calls fail at runtime |
| `stats-tool.query-access[n].query-pattern` / `.access` / `.groups[m].{name,type,role}` | No | shipped rules | Per-query access policy (`open`/`restricted`) (`statistics/StatsToolProperties.java`) |
| `analytics.url`, `analytics.token` | **Yes** | — (not shipped) | Matomo reporting API. **Secret** (token) |
| `spring.ai.openai.api-key` | Yes, for document generation | placeholder | **Secret** |
| `spring.ai.openai.chat.options.model` / `.temperature` | No | `gpt-5-mini` / `1.0` | Model. A model change triggers document regeneration |
| `google.recaptcha.secret` | **Yes** | empty | Startup fails if it's empty (recaptcha library). **Secret** |
| `google.recaptcha.version` / `.threshold` / `.url` / `.header` | No | `3` / `0.7` / Google siteverify / `g-recaptcha-response` | |
| `ws.allowedOriginPattern` | **Yes** for browsers | empty | Allowed origins for the STOMP endpoint |
| `sentry.dsn`, `sentry.environment`, `sentry.traces-sample-rate` | No | empty, empty, `0.1` | Error and performance reporting. **Secret** (DSN) |

## API documentation and privacy

| Property | Default | Description |
|---|---|---|
| `springdoc.group-configs[*]` | groups `observatory`, `catalogue`, `registry` | Swagger groups by package |
| `springdoc.swagger-ui.csrf.*` | enabled, cookie `XSRF-TOKEN` | Swagger UI sends the CSRF header |
| `openapi.info.*` | title, description, version, licence | OpenAPI metadata |
| `privacy.wrapper-classes[*]`, `privacy.entries[*]` | message emails, document metadata | Fields redacted from responses by `DataPrivacyAdvice` |

## Environments

| Environment | Notes |
|---|---|
| Local | Override credentials, OIDC registration, `observatory.*` (including `invitationKey` and `hmacSecret`), `analytics.*`, `mailer.from`, `stats-tool.endpoint`, `google.recaptcha.secret`. Keep `mail-debug` |
| Tests | `src/test/resources/application.properties`: HSQLDB for authorization and erasure, dummy secrets, Flyway on. Integration tests use Testcontainers PostgreSQL and Elasticsearch |
| Production | Real values for all secrets, `spring.profiles.active` without `mail-debug`, `observatory.surveySchedulerCron` if needed, the mailer and messaging API keys if those services set them. One instance only. Values are set per deployment through environment variables or an external configuration, not stored in the repository |

## Secrets management

Secrets: DB, Redis and Elasticsearch passwords, OIDC client secret, `observatory.invitationKey`,
`observatory.hmacSecret`, `mailer.client.api-key`, `messaging-system.client.api-key`,
`analytics.token`, `spring.ai.openai.api-key`, `google.recaptcha.secret`, `sentry.dsn`. Provide them as
environment variables (`OBSERVATORY_INVITATION_KEY` and `OBSERVATORY_HMAC_SECRET` are read by the
shipped file) or mounted files at container start. Back up `observatory.hmacSecret`. The shipped `application.properties`
contains only empty values or placeholders, and must stay that way. (values inspected
without disclosure)
