# EOSC Observatory Backend — Security

## Authentication

- OIDC login (`oauth2Login`) with session cookies stored in Redis (Spring Session). Cookie
  `http-only`, timeout 120 min. (`SecurityConfig`, `application.properties`, `pom.xml`)
- RP-initiated OIDC logout (`oidcLogoutSuccessHandler`), with session invalidation.
- Invitations are encrypted tokens (JWE `dir`/`A256GCM`, key `observatory.invitationKey`) that expire
  after `observatory.invitationTtl` (7 days). Only the invited email can accept one, and the inviter's
  right to invite is checked again on acceptance. (`InvitationServiceImpl`)

## Roles and authorisation

- `ADMIN`: users whose email (ID token, userinfo or OAuth2 attribute) is in `observatory.admins`.
  Nobody is `ADMIN` when the property is unset. (`SecurityConfig#userAuthoritiesMapper`)
- Group roles: administrator, coordinator, stakeholder manager and contributor. Checked by custom
  expressions in `MethodSecurityExpressionsService` and by `hasPermission(...)` against the
  authorization library's store.
- Answer permissions are granted and revoked by `PermissionsUpdateAspect` when answers are created
  or deleted. Live edits over WebSocket are re-authorised per message against current permissions.

- Messaging: the messaging service doesn't authorise users, so `MessagingSystemController` checks
  every operation and takes the user's identity from the session.

  | Operation | Check |
  |---|---|
  | Get a thread, search inbox/outbox, unread counts | Member of the requested group(s) |
  | Create a thread | Sender email is the user's own, and the user is a member of the sender group |
  | Add a message | Sender email is the user's own |
  | Mark a message read/unread | The user, or one of their groups, takes part in the thread |
  | Update or delete a thread | `ADMIN` |
  | `POST /threads/public` | Anonymous, reCAPTCHA v3 |

## URL rules

`SecurityConfig#filterChain`. The registry and catalogue controllers are component-scanned into this
application, so their endpoints are served under `/api` too.

| Rule | Paths |
|---|---|
| `permitAll` | `GET /forms/**` |
| `ADMIN` | `/logs/**`, `/forms/**`, `/dump/`, `/restore/`, `/resources/**`, `/resourceType/**`, `/search/**`, `/items/**`, `/version/**`, `/schemaService/**`, `/resourcesync/**` |
| `authenticated` | `/websocket`, `/request/**` (catalogue data proxy) |
| `permitAll` | everything else, relying on method security |

(code and `SecurityConfigUrlRulesTest`). Public paths that remain include
`GET /ui/vocabularies/map` (catalogue vocabularies) and the application endpoints without
`@PreAuthorize`.

## Actuator

`GET /api/actuator/health` returns the status to everyone and the component details only to users
with the `ADMIN` authority (`show-details=when-authorized`, `roles=ADMIN`).

## CSRF and CORS

- CSRF: session-backed token, header `X-XSRF-TOKEN`, mirrored to a non-`httpOnly` `XSRF-TOKEN`
  cookie (`secure` when the request is HTTPS). The token is also required on the WebSocket handshake.

- CORS: Spring's CORS support is disabled. `SimpleCORSFilter` sends credentialed CORS headers only
  to the origins listed in `observatory.corsAllowedOrigins`, which is empty by default. Requests from
  other origins get no CORS headers. (`SimpleCORSFilterTest`)
- WebSocket origins: `ws.allowedOriginPattern`.

## Input validation and CAPTCHA

- Survey answers are validated against their catalogue models.
- `POST /threads/public` (anonymous thread creation) requires reCAPTCHA v3 with threshold `0.7`.

- Document generation fetches URLs that users typed into survey answers. `document-analyzer` does
  this server-side with headless Chromium, so restrict egress at the network level.
- The catalogue data proxy (`POST /request/default`) fetches caller-supplied URLs. It needs login.

## Secrets

See [configuration.md](configuration.md#secrets-management). The shipped properties contain only
empty values or placeholders.

## Personal data

- Users, stakeholders, answers and messages hold personal data in PostgreSQL, Elasticsearch,
  registry version history, Redis and the messaging service.
- `DataPrivacyAdvice` redacts message emails and document metadata in responses (`privacy.*`).

- User purge (`DELETE /api/users/{id}`, `ADMIN`) removes the user from groups, survey answers and
  their version history, news items, survey definitions, documents, comments and the messaging
  service, and records each attempt in the erasure register (schema `erasure`). Operators must
  follow [user-erasure.md](user-erasure.md): the application doesn't revoke the user's sessions.

- Logs: an erased user's identifier is masked in the `user` log field, and permission debug logs
  pseudonymise the user with `observatory.hmacSecret`. Without the secret, log masking is disabled
  and purges fail.
- The `mail-debug` stub logs masked addresses.
- When `sentry.dsn` is set, Sentry receives errors and traces (`sentry.traces-sample-rate=0.1`).
  Configure data scrubbing.
