# EOSC Observatory Backend — Operations

## Start / stop

At startup the application:

1. Runs registry Flyway migrations and validates the registry schema.
2. Creates the resource types from `classpath:resourceTypes`.
3. Reconciles Elasticsearch with PostgreSQL in the background (registry `IndexDbSync`).
4. Warms up the embedding model.
5. Launches headless Chromium (`document-analyzer`).

Stop it with `SIGTERM`. Spring Boot's graceful shutdown is on by default (Boot 3.4+; this application
uses 3.5 and doesn't override `server.shutdown`). In-flight async document generation is interrupted.

## Health

`GET /api/actuator/health` returns the status. Component details are shown only to `ADMIN` sessions
(`show-details=when-authorized`). Datasource, Redis and disk indicators come from Spring Boot
auto-configuration. Spring Boot also adds an `elasticsearch` indicator, but it doesn't check the
registry's cluster: the registry creates its own `RestHighLevelClient` from `registry.elasticsearch.*`,
while Spring Boot, because the low-level REST client is on the class path, creates a separate
`RestClient` from `spring.elasticsearch.uris` (default `http://localhost:9200`) and checks that one.
This application sets neither `spring.elasticsearch.uris` nor
`management.health.elasticsearch.enabled`, so where nothing listens on `localhost:9200` the overall
status is `DOWN`. No dedicated liveness or readiness groups are configured.

## Logging

Log4j2 (`src/main/resources/log4j2.xml`), with Sentry's Log4j2 appender (`sentry-log4j2`).
`request-logging-starter` logs requests. Notable messages:

| Source | Meaning |
|---|---|
| registry `ResourceMonitor` `Error notifying listener` | The Elasticsearch index or version history missed a change |
| `SurveyAnswerCrudService` autosave errors | A cached answer couldn't be flushed. It's retried on the next run |
| `ResilientMailer` `Mail delivery attempt n/m failed` (WARN) / `Mail delivery failed after n attempt(s)` (ERROR) | The mailer was unreachable, timed out or rejected the mail. The ERROR line names the subject and the recipient count, not addresses |
| `EmailSurveyService` errors | Survey notification emails failed for a survey |
| `MailDebugConfig` output | `mail-debug` profile active: no emails are sent |
| `Erasure log masking is disabled` (WARN at startup) | `observatory.hmacSecret` is unset. Purges will fail |
| Purge report line (subject reference, attempt id, counts) | A user erasure finished. See [user-erasure.md](user-erasure.md) |

## Metrics and alerting

Sentry (errors and traces, `sentry.traces-sample-rate=0.1`). Actuator metrics aren't exposed over
HTTP.

## Background jobs

| Job | Schedule | Notes |
|---|---|---|
| Survey-answer autosave flush | every 120 s | Flushes Redis entries older than 10 minutes to the registry |
| Survey lifecycle emails | `observatory.surveySchedulerCron` | Disabled by default (`-`) |
| Presence cleanup | every 300 s | Drops presence entries idle for over an hour |
| Document generation | on demand, `@Async` | Calls websites and OpenAI. Cost scales with survey size |

Run exactly one instance. The schedulers, survey-answer locks and the purge lock are in-memory, so a
second instance would flush and email twice and could edit groups during a purge.

## Data, backups, restore

| Store | Backup |
|---|---|
| PostgreSQL `registry` (+ `commenting`, `external_datasets`, `erasure` schemas) | `pg_dump`. This is the system of record. The `erasure` schema is the audit trail of user erasures |
| PostgreSQL `permissions` | `pg_dump` |
| Redis | Sessions and unsaved autosaves (up to ~12 minutes of edits). Persist it if losing them is unacceptable |
| Elasticsearch | Derived. Rebuilt from PostgreSQL at startup |

Registry-level dumps (`/api/dump/`, `ADMIN`) give a logical export. Restoring a backup taken before
a user erasure brings the erased data back: repeat the erasure for the users completed in the
register since that backup.

## User erasure

Follow [user-erasure.md](user-erasure.md).
