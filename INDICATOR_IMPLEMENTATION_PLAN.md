# Indicator Implementation Plan — First Working Example

## Context

The `indicator/` semantic-model package (`IndicatorDefinition`, `IndicatorQueryValidator`,
`IndicatorQueryHandler`/`IndicatorQueryHandlerRegistry`) was built and reviewed. Separately,
`LegacyIndicatorCatalogMappingTest.java` proved that ~40+ real dashboard indicators across 8 topic
areas (Publications, Data Management, FAIR Data, Open Data, Repositories/EOSC-connection, Long-term
Data Preservation, Open-Source Software, Skills/Training, Citizen Science) collapse into a small set
of generic `IndicatorDefinition` shapes, and identified exactly where each one's data comes from
today: proxied through this backend's `/statistics/raw` endpoint to an external survey-stats service,
proxied to OpenAIRE's separate bibliometric stats-tool, or (in two cases) genuinely computed locally
by this backend from its own `SurveyAnswer`/`Stakeholder` storage.

None of this is wired end-to-end yet. `IndicatorQueryController`/`IndicatorCatalogController` are
unmapped skeletons, no `IndicatorQueryHandler` implementation exists beyond test doubles, and
`IndicatorAnalyticsRepository`/`RemoteIndicatorClient` have zero implementations. Meanwhile the
Angular dashboard (`observatory-ui`) works today by calling those external services directly with
hand-built query strings — the semantic layer isn't in that data path at all.

**Goal**: a concrete, checkable plan to reach a *first working example* — one real, Spring-wired,
HTTP-reachable indicator query path — so a future dynamic dashboard can ask for something like "ratio
of closed access publications" and get a real number back through `IndicatorDefinition`/
`IndicatorQuery`, instead of the UI hand-building an external query string itself. This is the
foundation for letting the UI generate widgets from user-chosen indicators + filters rather than from
hardcoded per-page components.

## Survey Model field audit — accepted answer values

The actual survey `Model` JSON (`m-eosc-sb-2024`) was checked field-by-field for every question this
catalog depends on, rather than assuming a shape.

**Policy/monitoring/financial-strategy questions (all topic areas)**: every one of these fields is
`"typeInfo":{"type":"radio","values":[{"id":"Yes","label":"Yes"},{"id":"No","label":"No"}]}` —
confirmed identical across `Question6-0`, `Question6-3`, `Question7-0`, `Question10-0`, `Question14-0`,
`Question18-0`, `Question30-0`, `Question38-0`, `Question22-0`, `Question42-0`, `Question50-0`,
`Question54-0`, `Question58-0`, `Question62-0`, `Question66-0`, `Question70-0`, `Question78-0`,
`Question86-0`, `Question90-0`, `Question98-0` and their `Practices`-section monitoring counterparts.

**Implication**: the raw survey value is the literal string `"Yes"`/`"No"`, not a native boolean — any
handler must map `"Yes"` → `true` / `"No"` → `false` explicitly. `IndicatorValueType.BOOLEAN` is the
right target type, but the source data is string-typed.

**Financial-investment questions**: `"typeInfo":{"type":"number","properties":{"decimals":2}}`,
consistently labeled "...in millions of Euros" (`Question56-0`, `Question60-0`, `Question64-0`,
`Question68-0`, `Question72-0`, `Question80-0`, `Question92-0`, `Question100-0`, `Question111-0`).

**Implication**: `UnitType.CURRENCY` alone is insufficient — the survey's own field label states the
values are *already in millions*, an implicit ×1,000,000 scale factor. This is a real-world
confirmation of the deferred `DisplayFormat`/`UnitDefinition` gap:
without a scale field, an indicator showing "16.11" is ambiguous between €16.11 and €16.11M. Treat as
a must-fix-before-launch item for these indicators, not a nice-to-have.

**"Count in EU countries" questions — real discrepancy found, do not model as-is**: `Question61-0`
(the field the UI's `Question61` fetch targets, "56. How many data management plans were
published...") is marked `"deprecated":true` in the current survey Model. The survey has since been
redesigned around a different, non-deprecated composite field (`Question61-3`, "56. How many and what
percentage of national funding programmes which mandate the use of DMPs...", a `number`+`percentage`
pair). This matches the UI-side finding that this tile is HTML-commented-out (dead code) — the survey
model moved on and the UI never followed.

**Decision: drop `data_management.dmp_count` from the catalog** rather than model a deprecated field
as `ACTIVE`. If revisited later, either mark it `IndicatorStatus.DEPRECATED` or model the
replacement composite field instead — a composite number+percentage pair doesn't fit a single
`ScalarValue` cleanly and needs its own design, out of scope here.

**"Types of publications" / access status — not in the survey Model at all**: checked, and the survey
Model has no such field. Publication access-status/type vocabulary (Open/Closed/Embargo, etc.) is
**OpenAIRE's own bibliometric API vocabulary** (`result.access mode`, `result.type`), external to this
survey entirely — matching indicator 67's already-established external sourcing. **Confirmed**:
`accessStatus` is not a binary OA/Closed split — it's `OA` vs three distinct non-open statuses,
`CLOSED`/`EMBARGOED`/`RESTRICTED` (not a single collapsed "Closed" bucket). Exact wire values against
OpenAIRE's live API contract still unverified this round.

## Indicator catalog

| Definition | Semantic type | Source | Handler | Notes |
|---|---|---|---|---|
| `publications.count` | MEASURE | OpenAIRE bibliometric | `OpenAireBibliometricIndicatorQueryHandler` | `accessStatus` filter values TBC against real OpenAIRE API |
| `publications.oa_share` | RATIO | OpenAIRE bibliometric | `OpenAireBibliometricIndicatorQueryHandler` | **First working example target** — `accessStatus` filter (`OA` vs `CLOSED`/`EMBARGOED`/`RESTRICTED`) selects which share is returned, directly satisfying "ratio of closed access publications" |
| `survey.eu_topic_coverage` (generalizes `oa.eu_country_coverage`) | RATIO | Survey stats-tool (proxied) | `SurveyStatsToolIndicatorQueryHandler` | `topic`+aspect filter — confirmed Yes/No source shape above |
| `survey.country_topic_status` (generalizes `oa.country_initiative_status`) | ATTRIBUTE | Survey stats-tool (proxied) | `SurveyStatsToolIndicatorQueryHandler` | same source, per-country instead of aggregated |
| `survey.financial_investment` | MEASURE | Survey stats-tool (proxied) | `SurveyStatsToolIndicatorQueryHandler` | confirmed decimals:2, "in millions" scale — needs `DisplayFormat` before real launch |
| `repositories.long_term_preservation_count`, `citizen_science.new_projects_count` | MEASURE | Survey stats-tool (proxied) | `SurveyStatsToolIndicatorQueryHandler` | kept — underlying fields confirmed non-deprecated |
| ~~`data_management.dmp_count`~~ | — | — | — | **dropped** — see field-audit finding above |
| `survey.countries_with_validated_answers` | MEASURE | **Local** (`SurveyServiceImpl.getCountriesWithValidatedAnswer`) | `LocalRegistryIndicatorQueryHandler` | genuinely backend-computed (filter+map over local storage, no HTTP) |
| `stakeholder.participating_countries_count` | MEASURE | **Local** (`StakeholderController.getStakeholderCountryCodesByType`) | `LocalRegistryIndicatorQueryHandler` | same handler, different local source method |

## Handler architecture

- [ ] **`OpenAireBibliometricIndicatorQueryHandler`** — implements `IndicatorQueryHandler` +
      `RemoteIndicatorClient`. New `WebClient`/`RestTemplate` call to
      `https://services.openaire.eu/stats-tool/raw?json=...`, translating the query-DSL shape already
      proven working in `observatory-ui/.../explore-queries.ts` (`OAPublicationVSClosed`) into Java.
      `providerIndicatorKey` distinguishes `publications.count` vs `publications.oa_share`; the
      `accessStatus` filter value picks which share is returned — `OA` vs one of `CLOSED`/
      `EMBARGOED`/`RESTRICTED`, not a single complementary "Closed" bucket (four real statuses, not two)
- [ ] **`SurveyStatsToolIndicatorQueryHandler`** — implements `IndicatorQueryHandler`. Delegates to
      the existing, already-wired `StatsToolWrapperController` internally (reuse, don't reimplement
      the external call) or a direct `WebClient` against the same `stats-tool.endpoint` config
      property. `providerIndicatorKey` = survey question code. Performs the Yes/No→boolean mapping
      and, for coverage RATIO queries, the percentage aggregation currently done client-side in
      Angular
- [ ] **`LocalRegistryIndicatorQueryHandler`** — implements `IndicatorQueryHandler` +
      `IndicatorAnalyticsRepository` (the port meant for local computation, currently unimplemented).
      Wraps `SurveyServiceImpl`/`StakeholderService` calls directly (constructor-injected), no HTTP
      client at all — the genuinely-local case

## Spring wiring (needed for a first working example, not full coverage)

- [ ] Add real `@RestController`/`@RequestMapping` annotations to `IndicatorQueryController`
      (currently an unmapped skeleton) — one route, e.g. `POST /api/v1/indicators/query`, accepting an
      `IndicatorQuery` body and an authenticated `ActorContext`
- [ ] `IndicatorDefinitionLookup`/`DimensionDefinitionLookup` need a concrete implementation —
      recommend an in-memory, `@Component`-annotated class backed by a static `List<IndicatorDefinition>`
      (the catalog above), since persistence is still an explicit open question
      — do not block the first working example on a persistence decision
- [ ] `IndicatorQueryHandlerRegistry` gets a `@Bean` constructed from the concrete handler list
- [ ] `AllowAllIndicatorAuthorizationService` (already built) is fine as the first-pass authorization
      implementation

## First working example — concrete target

Wire `publications.oa_share` end-to-end:

```
POST /api/v1/indicators/query
{"indicatorCode":"publications.oa_share","filters":[{"dimension":"accessStatus","operator":"EQ","values":["CLOSED"]}]}
```

→ validator compiles a plan → `OpenAireBibliometricIndicatorQueryHandler` calls the real OpenAIRE
stats-tool → returns a real percentage. This directly answers "ratio of closed access publications"
and requires only one handler (not all three), keeping the first slice small.

- [ ] `publications.oa_share` end-to-end path returns a real number from a real HTTP call

## Testing plan

- [ ] Unit tests for the query-DSL translation (pure function, no network) — verify the built JSON
      string matches the shape `OAPublicationVSClosed` produces in `explore-queries.ts`
- [ ] `OpenAireBibliometricIndicatorQueryHandler` tested against a stubbed HTTP layer (WireMock or a
      fake `WebClient`) — no live network calls in the test suite
- [ ] Extend `LegacyIndicatorCatalogMappingTest`-style validator-compilation tests for the new catalog
      entries in the table above
- [ ] A unit test for `LocalRegistryIndicatorQueryHandler` using existing local test doubles for
      `SurveyServiceImpl`/`StakeholderService` (no HTTP)

## Open risks / unresolved before implementation

- [ ] OpenAIRE stats-tool query-DSL and response shape not independently verified against a live call
      this round (translated from the existing working Angular code, which is good evidence, but the
      Java translation itself is unverified)
- [x] `accessStatus` allowed values — confirmed: `OA` vs `CLOSED`/`EMBARGOED`/`RESTRICTED` (not a
      binary OA/Closed split). Exact wire-format values still to confirm against OpenAIRE's API docs.
- [ ] EU-country-membership source for coverage denominators (`Stakeholder.associationMember`/
      `mandated` vs. a hardcoded list) — still unresolved
- [ ] External survey stats-tool's raw response shape (country-coded or not) — `stats-tool.endpoint`
      is unset in this checkout, so nothing was sampled
- [ ] `data_management.dmp_count` dropped pending the composite-field redesign question above

## Phased rollout

- [ ] **Phase A** — catalog + handler classes, unit-testable, no Spring wiring, no live HTTP
- [ ] **Phase B** — Spring-wire `IndicatorQueryController`, implement
      `OpenAireBibliometricIndicatorQueryHandler` for real, ship the one first-working-example path
      above
- [ ] **Phase C** (future, not detailed here) — remaining handlers, broader catalog coverage, UI
      migration off `EoscReadinessDataService`'s direct external calls, toward a dynamic
      indicator-driven dashboard
