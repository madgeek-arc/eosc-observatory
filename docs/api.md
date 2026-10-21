# EOSC Observatory Backend — API

The generated OpenAPI document is the canonical endpoint reference. This page gives only an
overview.

| Item | Value |
|---|---|
| Base URL | `http(s)://<host>:8280/api` |
| Swagger UI | `/api/swagger-ui/index.html` (springdoc 2.8.9) with groups *1. EOSC Observatory* (`eu.openaire.observatory`), *2. Dynamic Catalogue* (`gr.uoa.di.madgik.catalogue`), *3. Registry Core* (`gr.uoa.di.madgik.registry`) |
| OpenAPI JSON | `/api/v3/api-docs` (or `/api/v3/api-docs/<group>`). Generated at runtime, not version-controlled. The version comes from `@project.version@` |
| Authentication | Session cookie from the OIDC login flow (`/api/oauth2/authorization/<registration>`, Spring Security defaults). Logout at `GET`/`POST /api/logout` |
| CSRF | `X-XSRF-TOKEN` header on state-changing requests. Token from the `XSRF-TOKEN` cookie or `GET /api/csrf`. Swagger UI sends it automatically |
| Authorisation | `@PreAuthorize` per endpoint, plus the URL rules in [security.md](security.md#url-rules) |
| Errors | JSON from `ExceptionControllerAdvice` (extends catalogue's `GenericExceptionController`) |
| Versioning | None in paths. The API version equals the application version |

## Major groups (application controllers)

| Area | Controllers (`src/main/java/eu/openaire/observatory/...`) |
|---|---|
| Organisation | `controller/AdministratorController`, `CoordinatorController`, `StakeholderController`, `GroupsController`, `InvitationController` (`POST /invitation` creates and emails an invitation, `POST /invitation/accept` accepts one) |
| Surveys | `controller/SurveyController`, `SurveySettingsController`, `SurveyAnswerCommentController`, `CsvController` |
| Users and policy | `controller/UserController` (including `DELETE /users/{id}`, the `ADMIN` user purge), `PrivacyPolicyController`, `PermissionController`, `CsrfController` |
| Content | `controller/DashboardController` (`/dashboards/{code}/types/...`: widget defaults per stakeholder type and per-stakeholder overrides), `NewsItemController`, `datasets/DatasetController`, `resources/ResourcesController`, `resources/analyzer/DocumentAnalyzerController` |
| Integrations | `messaging/MessagingSystemController` (threads, inbox/outbox, `POST /threads/public` with reCAPTCHA), `statistics/StatsToolWrapperController`, `controller/AnalyticsController` |
| Realtime | STOMP destinations in [integration.md](integration.md#frontend-websocket) |

Embedded registry and catalogue controllers also appear under `/api` (see [security.md](security.md)
for their access rules).
