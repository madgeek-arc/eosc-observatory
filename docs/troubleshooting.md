# EOSC Observatory Backend — Troubleshooting

| Problem | Possible cause | What to check | Resolution |
|---|---|---|---|
| Startup fails: `Could not resolve placeholder 'analytics.url'` (or `analytics.token`) | Keys not shipped and no default | Effective configuration | Set both |
| Startup fails: `observatory.invitationKey is not set` / `is not valid base64` / `must decode to 32 bytes` | Invitation key missing or malformed | Configuration, `OBSERVATORY_INVITATION_KEY` | Set a key from `openssl rand -base64 32`, and keep it stable |
| User purge fails: `observatory.hmacSecret is not configured` | No HMAC secret | Startup warning `Erasure log masking is disabled` | Set `observatory.hmacSecret` (`OBSERVATORY_HMAC_SECRET`) and retry the purge |
| Group edits return `409` `User erasure is in progress` | A user purge is running | Admin activity | Wait for the purge, reload the group and retry ([user-erasure.md](user-erasure.md)) |
| Startup fails: `reCAPTCHA secret not provided` | `google.recaptcha.secret` empty | Configuration | Set the secret |
| Startup fails on registry schema validation | `hbm2ddl.auto=validate` against an empty or old database | Hibernate validation error | Bootstrap or migrate the registry schema first |
| Startup fails launching Chromium | Running outside the Docker image without Playwright browsers or OS libraries | Stack trace from `WebPageContentExtractor` | Use the image, or install Playwright Chromium (`install --with-deps chromium`) |
| Login works but the user has no admin rights | `observatory.admins` unset or the email isn't listed | The configured emails | Set `observatory.admins` |
| No emails sent | `mail-debug` profile active (default) | Startup profiles, `MailDebugConfig` logs | Set `spring.profiles.active` without `mail-debug` |
| Emails not received, `Mail delivery failed after n attempt(s)` in the logs | Mailer unreachable or slow, wrong `mailer.client.host`, `401` (API key), or `502` (SMTP delivery failed in the mailer) | The logged root cause, mailer logs | Fix the host, set `mailer.client.api-key` to the mailer's `mail.api-key`, or fix the mailer's SMTP settings |
| Requests are slow while mail is sent | Mail is sent synchronously, with up to 5 attempts | `Mail delivery attempt n/m failed` warnings | Fix the mailer, or lower `mailer.retry.*` / `mailer.client.*-timeout` |
| `403` on POST/PUT/DELETE | Missing or stale CSRF token | `X-XSRF-TOKEN` header vs the `XSRF-TOKEN` cookie | Fetch `GET /api/csrf` and resend |
| WebSocket connection refused | Origin not in `ws.allowedOriginPattern`, unauthenticated session, or bad CSRF token | Browser console, server logs | Fix the origin pattern. Log in first |
| Messaging endpoints fail | `messaging-system.client.endpoint` empty or wrong, or the messaging service requires an API key (`401`) | Client warning at startup, `WebClientResponseException` status | Set the endpoint including `/api/`, and `messaging-system.client.api-key` to the service's key |
| `403` when marking a message read | The user and their groups don't take part in the thread | Thread recipients | Expected for non-participants |
| `403` when creating a thread | `from.email` isn't the logged-in user's email, or the user isn't in `from.groupId` | Request body | Send the user's own email and one of their groups |
| Browser blocks cross-origin requests | The frontend's origin isn't in `observatory.corsAllowedOrigins` | Browser console, `Origin` header | Serve the frontend from the same origin, or add its origin |
| `/api/version/**`, `/api/resourcesync/**` or `/api/schemaService/**` redirect to login or return `403` | Restricted to `ADMIN` | Session | Use an `ADMIN` account |
| Search results stale | Registry index listener failed | `Error notifying listener` in logs | Fix Elasticsearch, then restart (reindex at startup) |
| Recent edits lost after a restart | Autosaves still in Redis (flushed after 10 minutes) and Redis was cleared | Redis persistence | Enable Redis persistence. Don't flush Redis on deploy |
| Document generation slow or failing | Websites slow or blocking, OpenAI key or quota | `document-analyzer` and `SurveyAnswerDocumentAnalyzer` logs | Check the API key and quota and outbound network |
| Statistics charts empty or errors | `stats-tool.endpoint` empty, or the query isn't allowed by `stats-tool.query-access` | Logs from `StatsToolWrapperController` | Set the endpoint. Add an access rule |
