# Indicator package — open TODOs

Forward-looking items only. Superseded `INDICATOR_INVENTORY_STATUS.md` and
`INDICATOR_REFACTORING.md`, whose remaining content was either session history/verification logs
(no longer useful) or already resolved in code. See `indicator/README.md` for the contributor
guide and `docs/INDICATORS.md` for the API reference; `docs/INDICATOR_MIGRATION_STATUS.md` is kept
separately for the `observatory-ui` migration effort.

- [ ] **(Medium-High) `securityScope`/`limit` are computed by the validator but never applied by any
  handler.** `IndicatorQueryValidator.compile` resolves `QuerySecurityScope` and `limit` onto every
  `IndicatorExecutionPlan`, and `IndicatorQueryHandler`'s own javadoc says implementations "are
  expected to apply `plan.securityScope()`" — but none of the three concrete handlers
  (`openaire/OpenAireStatsToolIndicatorQueryHandler`, `survey/SurveyStatsToolIndicatorQueryHandler`,
  `local/LocalRegistryIndicatorQueryHandler`) reference `plan.securityScope()` or `plan.limit()`
  anywhere. Harmless only because `AllowAllIndicatorAuthorizationService` always returns an
  unrestricted scope today — the moment real authorization lands, row-level scoping silently does
  nothing unless every handler is updated by hand, and nothing would catch a handler that forgets.
  `limit` is validated/defaulted (`DEFAULT_LIMIT = 1000`) but never truncates
  `IndicatorResult.data()` — pure decoration today. Action: add a wiring-level test that fails if a
  handler ignores a non-unrestricted `securityScope`, so this can't silently rot before real
  authorization is implemented.

- [x] **(Medium) Filter *values* aren't validated against their dimension's real member set.**
  `IndicatorQueryValidator.resolveFilter` now checks `EQ`/`NOT_EQ`/`IN`/`NOT_IN` filter values
  against the real member set of any dimension with a registered `DimensionMemberProvider`, throwing
  the new `InvalidFilterValueException` (400) instead of letting a bad value reach handler code.
  `DimensionMemberProvider` gained a `validCodes(String indicatorCode)` default method — it just
  re-runs `search()` with no search text and an unbounded limit and collects the codes, so every
  provider gets a real, always-current value set for free from whatever already backs its `search()`
  (an enum, a hardcoded list, a DB query, or a live/cached OpenAIRE call) with no per-provider
  overrides needed. This means validation now applies uniformly to *every* dimension with a
  provider, including `country`/`surveyId`/`classification`/`fieldOfScience` (previously left
  unvalidated as "open" under an earlier, since-discarded design). `DimensionMemberProviderRegistry.find`
  looks the provider up. Covered in `IndicatorQueryValidatorTest`, `DimensionMemberProviderRegistryTest`,
  and one `validCodes()` regression assertion each in `AccessStatusDimensionMemberProviderTest`/
  `InitiativeTypeDimensionMemberProviderTest`.

- [ ] **(Medium) "Coverage of Researchers by National Open Science Policies" — indicator + preset
  registered, query path still stubbed.** `oa.researcher_policy_coverage`
  (`IndicatorCodes.RESEARCHER_POLICY_COVERAGE`) is now in `IndicatorDefinitionCatalog` and backs
  preset `osp-5`, so `GET /indicators/presets` covers the inventory row — but
  `LocalRegistryIndicatorQueryHandler` answers a query for it with
  `IndicatorNotYetImplementedException` (HTTP 501). The remaining work is the data wiring below.
  Mechanism confirmed real, data population unconfirmed: `eu.openaire.observatory.datasets.DatasetEntry`
  (fields: `year`, `country`, `name`, `authority`, `value`) backs a EUROSTAT-sourced ingest
  (`ingest/custom/CustomIngestService`/`CustomCsv`) that, on a TSV upload with a non-blank
  `researchers` column, saves one row per (year, country) with `name == "researchers"` and
  `authority == "EUROSTAT"` — a plausible per-country denominator for this row. But: there is no
  Flyway/Liquibase migration, seed data, or test fixture anywhere in the repo for this data (schema
  is Hibernate `hbm2ddl.auto=update`-only) — whether any live instance's `external_datasets` schema
  actually has populated `"researchers"` rows is an operational fact, not something the repo
  guarantees. Also, **no `IndicatorQueryHandler` reads `datasets` today** — `grep` confirms zero
  references from the `indicator` package tree into `datasets`. Next step if picked up: extend
  `LocalRegistryIndicatorQueryHandler` (`indicator/local/`) with a new case injecting `DatasetService`
  for the researcher-count side, combined with `IndicatorDefinitionCatalog.policyAreasCount()`
  (`POLICY_AREAS_COUNT`/`oa.policy_areas_count`, the existing per-country "how many topic areas have
  a Yes national-policy answer" indicator) for the policy-coverage side — likely
  `sum(researchers[country] with ≥1 policy) / sum(researchers[country] across all countries)`.
  Replace the stub `case IndicatorCodes.RESEARCHER_POLICY_COVERAGE ->` in
  `LocalRegistryIndicatorQueryHandler` with that logic once the EUROSTAT `"researchers"` data is
  confirmed populated.

- [ ] **(Low-Medium) Add `REPOSITORIES_PRESERVATION_FINANCIAL_STRATEGY` (initiativeType) and
  `REPOSITORIES_PRESERVATION` (investmentArea) dimension members**, each live-`curl`-verified
  against the survey stats-tool for 2022/2023 first. Needed to back "National Financial Strategy on
  Long-term Data Preservation" (Question39) and "Financial investments in Long-term Data
  Preservation" (Question88) — the only 2 of 69 original inventory rows still excluded from
  `IndicatorCatalog` (see its class javadoc). The preservation sub-topic currently only has
  policy/monitoring initiativeType members
  (`REPOSITORIES_PRESERVATION_POLICY`/`REPOSITORIES_PRESERVATION_MONITORING_INITIATIVE`).

- [x] **(Low) `CountryDimensionMemberProvider` uses the deprecated `Locale(String, String)`
  constructor.** Swapped `new Locale("", code)` for `Locale.of("", code)` in
  `dimension/CountryDimensionMemberProvider.java`.

- [ ] **(Medium) Adopt stats-tool `in` / `not_in` operators in `OpenAireQueryBuilder`** — collapse the
  current OR-of-`=` multi-leaf blocks into a single `type:"in"` leaf for multi-value dimension filters.
  Blocked on the `in`/`not_in` stats-tool build (`Statistic-chart-generator` `7645b1c`) being deployed
  behind `openaire.stats-tool.endpoint`. Full plan, affected methods, test list and rollout sequence in
  `docs/INDICATOR_IN_OPERATOR_MIGRATION.md`.

- [ ] **(Low) `SurveyIdDimensionMemberProvider` fetches up to 10,000 surveys before
  filtering/limiting in memory.** `FacetFilter.setQuantity(10000)` is unconditional; the caller's
  `search`/`limit` is applied only afterward, in memory (`DimensionMemberSearch.filter`). Fine at
  current survey volumes; push search/limit down into the `FacetFilter` query itself if that
  changes.

- [ ] **(Informational) `PUBLIC` access level with a no-op authorization layer is production-wired,
  not a bug.** `AllowAllIndicatorAuthorizationService`/`AllowAllActorContext` are real `@Service`
  beans in production; every indicator in `IndicatorDefinitionCatalog` — including the `oa.*` survey
  indicators whose underlying questions are RESTRICTED-tier via `StatsToolWrapperController` — is
  reachable at `PUBLIC` with no real enforcement. Deliberate and documented (`docs/INDICATORS.md`,
  "Access control" section). Tracked here so it's one item, not rediscovered piecemeal, whenever
  `IndicatorAuthorizationService` gets a real implementation.
