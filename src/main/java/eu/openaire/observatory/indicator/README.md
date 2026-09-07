# Adding a new indicator

This is a contributor guide for extending the indicator semantic layer in this package — how to add
a new indicator, and, if needed, a new dimension or a new handler behind it. For the HTTP-facing API
(what a *consumer* of `/api/indicators` and `/api/indicators/query` needs to know), see
[`docs/INDICATORS.md`](../../../../../../../docs/INDICATORS.md) instead.

## Request flow

```
IndicatorQueryController
  -> IndicatorQueryService.execute(query, actor)
       -> IndicatorQueryValidator.compile(query, actor)   // raw IndicatorQuery -> IndicatorExecutionPlan
       -> IndicatorQueryHandlerRegistry.getRequired(handlerKey)
       -> handler.execute(plan)                           // IndicatorExecutionPlan -> IndicatorResult
       -> TimeSeriesNormalizer.normalize(result, timeRange)
```

Handlers never see the raw `IndicatorQuery` — only the validated `IndicatorExecutionPlan`. Anything
the validator doesn't check (see [What the validator does *not* check](#what-the-validator-does-not-check-your-handler-must)
below) is the handler's own responsibility.

## Mental model: the two registries

Both extension points in this package follow the same pattern: implement an interface as a Spring
bean, and it's picked up automatically — there is no registry file to edit by hand.

**`IndicatorQueryHandlerRegistry`** (`handler/IndicatorQueryHandlerRegistry.java`) — Spring injects
every `IndicatorQueryHandler` bean as a `List<IndicatorQueryHandler>`, which the registry turns into
a `Map<String, IndicatorQueryHandler>` keyed by each bean's `handlerKey()`:

```java
public IndicatorQueryHandlerRegistry(List<IndicatorQueryHandler> handlers) {
    this.handlers = handlers.stream()
        .collect(Collectors.toUnmodifiableMap(IndicatorQueryHandler::handlerKey, Function.identity()));
}
```

**`DimensionMemberProviderRegistry`** (`dimension/DimensionMemberProviderRegistry.java`) — identical
pattern for `DimensionMemberProvider` beans, keyed by `dimensionCode()`.

Routing is two-level, via `IndicatorExecutionBinding(handlerKey, providerIndicatorKey)`:
- `handlerKey` picks the **handler bean** out of the registry.
- `providerIndicatorKey` is whatever that handler's *own* internal logic (typically a `switch`) uses
  to pick indicator-specific behavior — one handler commonly serves several indicators.
  `OpenAireStatsToolIndicatorQueryHandler` serves both `publications.count` and
  `publications.access_status_share` this way:

```java
public IndicatorResult query(IndicatorExecutionPlan plan) {
    String providerIndicatorKey = plan.definition().executionBinding().providerIndicatorKey();
    return switch (providerIndicatorKey) {
        case IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE -> queryAccessStatusShare(plan);
        case IndicatorCodes.PUBLICATIONS_COUNT -> queryPublicationsCount(plan);
        default -> throw new IllegalStateException(
            "OpenAireStatsToolIndicatorQueryHandler has no logic for providerIndicatorKey: " + providerIndicatorKey);
    };
}
```

## Walkthrough: adding a new indicator on existing dimensions

Worked example: `publications.avg_authors` — average authors per publication, filterable/groupable by
`country`. Reusing existing dimensions (`country`, `accessStatus`, `initiativeType`) is always cheap —
no dimension code changes at all. Reusing an existing *handler*, on the other hand, is **not**
automatically free: both handlers today (`OpenAireStatsToolIndicatorQueryHandler`,
`SurveyStatsToolIndicatorQueryHandler`) dispatch per-indicator via an explicit `switch` on
`providerIndicatorKey()` (see [Mental model](#mental-model-the-two-registries) above) — there's no
generic fallback, so every new indicator on either one needs a new `case` plus real query logic. Steps
1–5 below (the catalog entry) are the cheap, mechanical part; step 6 is where the actual work is.

**Step 1 — add a code constant** to `IndicatorCodes.java`, next to the others. Indicator and dimension
codes deliberately live here rather than on `IndicatorDefinitionCatalog` itself, so a handler in a different
package can reference them without pulling in catalog internals:

```java
public static final String PUBLICATIONS_AVG_AUTHORS = "publications.avg_authors";
```

**Step 2 — write a private static factory method** returning a fully-built `IndicatorDefinition`.
The record has 17 components — walk through them with a concrete example
(`model/IndicatorDefinition.java`):

```java
public record IndicatorDefinition(
    UUID id,
    String code,                 // stable external identifier, e.g. "publications.count"
    String label,
    String description,
    IndicatorValueType valueType,
    IndicatorSemanticType semanticType,
    UnitType unit,
    IndicatorFormat format,      // how to format the value: percentage/number/currency/category/yes-no
    RenderHint renderHint,
    AggregationPolicy aggregationPolicy,
    RatioPolicy ratioPolicy,     // nullable; meaningful only when semanticType == RATIO
    Set<IndicatorDimensionBinding> dimensionBindings,
    TimePolicy timePolicy,
    IndicatorExecutionBinding executionBinding,
    IndicatorAccessLevel accessLevel,
    IndicatorStatus status,
    int version
)
```

```java
/** Average number of authors per publication, country filterable/groupable. */
private static IndicatorDefinition publicationsAvgAuthors() {
    return new IndicatorDefinition(
        UUID.randomUUID(), PUBLICATIONS_AVG_AUTHORS, "Average Authors per Publication",
        "Average number of authors per published work, filterable/groupable by country.",
        IndicatorValueType.DECIMAL, IndicatorSemanticType.MEASURE, UnitType.COUNT, IndicatorFormat.NUMBER, RenderHint.SCALAR,
        new AggregationPolicy(
            AggregationType.AVG,
            Set.of(AggregationType.AVG, AggregationType.MIN, AggregationType.MAX),
            NullHandling.EXCLUDE
        ),
        null, // not a RATIO indicator -> no RatioPolicy
        Set.of(countryBinding()), // reuse the existing helper, see below
        TIME_POLICY,
        new IndicatorExecutionBinding(OpenAireStatsToolIndicatorQueryHandler.HANDLER_KEY, PUBLICATIONS_AVG_AUTHORS),
        IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
    );
}
```

(`PUBLICATIONS_AVG_AUTHORS` here is the constant from `IndicatorCodes`, brought into unqualified scope
via `import static eu.openaire.observatory.indicator.IndicatorCodes.*;` at the top of
`IndicatorDefinitionCatalog.java`, the same way every other indicator/dimension code constant is used throughout
that class.)

Notes:
- `ratioPolicy` is `null` unless `semanticType == RATIO` — see `publications.access_status_share` for
  a RATIO example (`new RatioPolicy(ZeroDenominatorHandling.NULL)`).
- `dimensionBindings` is a `Set<IndicatorDimensionBinding>`. Reuse the existing `countryBinding()`
  private helper when your binding shape matches it exactly; otherwise inline a
  `new IndicatorDimensionBinding(dimensionCode, usages, allowedOperators, requiredFilter)`.
- `executionBinding`'s first argument is the **handler key**, not the indicator code. There's only one
  handler today (`OpenAireStatsToolIndicatorQueryHandler.HANDLER_KEY`) — pointing a new indicator at
  it doesn't make the indicator work by itself, though; see step 6.

**Step 3 — register it** in `allDefinitions()`:

```java
private static List<IndicatorDefinition> allDefinitions() {
    return List.of(publicationsCount(), publicationsAccessStatusShare(), publicationsAvgAuthors());
}
```

**Step 4 — update the structural test.**
`IndicatorDefinitionCatalogTest.containsAllSeventeenRealDefinitions` asserts an exact count and code
list — it (and its name) need updating when another definition is added. Add a dedicated
assertion block for the new indicator's bindings, following the existing per-indicator test methods in
that file (e.g. `publicationsCountIsCountryFilterableAndGroupable`). No change is needed to
`everyDimensionBoundByEveryDefinitionIsRegistered` — it iterates `catalog.getAll()` automatically and
will just start covering the new definition.

**Step 5 — add a validator-wiring test.** `IndicatorDefinitionCatalogOpenAireWiringTest` is the template: build
a real `IndicatorQuery`, run it through a real `IndicatorQueryValidator`, and assert on the resulting
`IndicatorExecutionPlan` (required filters present, allowed operators accepted, groupBy resolves). This
is the right place to catch binding mistakes (wrong `DimensionUsage`, wrong `allowedOperators`) before
ever touching the handler.

**Step 6 — wire it into the handler.** Unlike dimensions, there's no generic fallback here: add a new
`case IndicatorCodes.PUBLICATIONS_AVG_AUTHORS -> queryAvgAuthors(plan);` to
`OpenAireStatsToolIndicatorQueryHandler.query()`'s `switch`, and a `queryAvgAuthors(...)` method —
`queryPublicationsCount`/`queryAccessStatusShare` are the two existing patterns to copy from (simple
measure vs. ratio). If the new indicator's data doesn't come from the OpenAIRE stats-tool
at all, it needs a whole new handler instead — see
[Adding a new handler](#reference-adding-a-new-handler-new-data-source).

If the indicator must be *registered* (so it shows up in the catalog and presets) before its data
source exists, the `case` may instead `throw new IndicatorNotYetImplementedException(...)` — that
maps to HTTP 501 with the real message. `oa.researcher_policy_coverage` in
`LocalRegistryIndicatorQueryHandler` is the current example.

## What the validator already guarantees (and what it does *not*)

All of the validation exceptions below surface to the client as a proper HTTP error with their real
message: the `BAD_REQUEST` ones extend `IndicatorValidationException` (→ catalogue
`ValidationException`, 400), the "unknown code" ones extend registry `ResourceNotFoundException`
(404), and `IndicatorAccessDeniedException` extends Spring Security `AccessDeniedException` (403).
A plain `RuntimeException` would instead be masked by `GenericExceptionController` as
`"Internal Server Error"` and logged at ERROR level.

`validation/IndicatorQueryValidator.compile(query, actor)` runs before any handler sees the query.
It checks, in order (`validation/IndicatorQueryValidator.java`):

1. The indicator code exists (else `UnknownIndicatorException`).
2. `actor.hasAccess(definition.accessLevel())` (else `IndicatorAccessDeniedException`).
3. Authorization produces the trusted `QuerySecurityScope` (currently
   `AllowAllIndicatorAuthorizationService` — unrestricted).
4. Requested aggregation is allowed — for non-RATIO indicators only; RATIO indicators skip this check
   entirely, since generic aggregation doesn't apply to a ratio (else `InvalidAggregationException`).
5. Every filter's dimension has a `FILTER`-usage binding, and its operator is in **both** the
   binding's `allowedOperators()` **and** the shared `DimensionDefinition`'s own
   `supportedOperators()` (else `UnsupportedDimensionException` / `UnsupportedOperatorException`).
6. Every `requiredFilter=true` binding has a matching filter present (else `MissingRequiredFilterException`).
7. Every `groupBy` dimension has a `GROUP`-usage binding (else `UnsupportedDimensionException`).
8. If `timeSeries` was requested, `definition.timePolicy().supported()` is true (else
   `TimeSeriesNotSupportedException`).
9. `timeSeries.from`/`to` are both present and satisfy `2015 <= from <= to <= <current year>` (else
   `InvalidTimeRangeException`).

### What the validator does *not* check — your handler must

- **Which** value(s) a required filter actually contains, beyond "a filter of that dimension is
  present." E.g. `publications.access_status_share` requires an `accessStatus` filter, but the
  validator doesn't know the handler's `switch` needs it to disambiguate numerator vs. denominator —
  `OpenAireStatsToolIndicatorQueryHandler` re-fetches it from `plan.filters()` itself and throws
  its own `IllegalStateException` if the operator is something it doesn't handle.
- **Ratio semantics** entirely — numerator/denominator resolution, `RatioPolicy.zeroDenominatorHandling()`.
  There is no generic ratio orchestration.
- **`limit`** enforcement on returned rows — `plan.limit()` is resolved but not auto-applied to the result.
- **`plan.securityScope()`** — computed by the validator but not automatically applied to the
  underlying query; a real (non-mock) handler must apply it itself. See the `IndicatorQueryHandler`
  interface javadoc.
- **Mapping catalog codes to source-specific literals**, e.g. `OpenAireAccessMode.fromCatalogCode(...)`
  translating the catalog's `"OA"`/`"CLOSED"`/... into whatever the remote source expects.

## Reference: adding a new dimension

Skip this if your indicator only needs `country` or `accessStatus` — reuse those (see the walkthrough
above).

1. Add a code constant to `IndicatorCodes.java`, then a `DimensionDefinition` entry to
   `dimensionsByCode` in `IndicatorDefinitionCatalog`'s constructor:

   ```java
   public record DimensionDefinition(
       String code, String label, DimensionType type,
       boolean filterable, boolean groupable, boolean sortable,
       Set<FilterOperator> supportedOperators,
       String parentDimensionCode // nullable; reserved, unused today
   )
   ```

2. Implement `DimensionMemberProvider` as a new `@Service` — auto-discovered by
   `DimensionMemberProviderRegistry`, no manual wiring needed:

   ```java
   public interface DimensionMemberProvider {
       String dimensionCode(); // used by the registry to dispatch
       DimensionMemberPage search(String indicatorCode, String searchText, int limit);
   }
   ```

   Three existing patterns to copy from, depending on where the member list comes from:
   - **Mirrors a closed Java enum** — extend `dimension/EnumDimensionMemberProvider.java` (e.g.
     `dimension/AccessStatusDimensionMemberProvider.java`, `GoldOaDimensionMemberProvider.java`).
     Use this whenever the dimension's members are really a fixed, small vocabulary that's already
     modeled as an enum for query-building (e.g. `openaire/OpenAireAccessMode.java`) — the member
     list is derived from the enum's constants, so it can't drift out of sync by hand.
   - **Static/hardcoded list, no backing enum** — `dimension/InitiativeTypeDimensionMemberProvider.java`
     (4 fixed `DimensionMember`s with no corresponding query-building enum to derive from).
   - **Delegates to another service** — `dimension/CountryDimensionMemberProvider.java` (derives
     members from `StakeholderService.getAllCountryCodes()`).

   Note: an "open vocabulary" dimension whose values come from OpenAIRE's own data (not a fixed
   enumeration we define) — like `classification`/`fieldOfScience` — should instead fetch live via
   `dimension/OpenAireDimensionMemberFetcher.java` (see `ClassificationDimensionMemberProvider.java`),
   not be hardcoded or enum-mirrored.

## Reference: adding a new handler (new data source)

Needed when neither existing handler (`openaire-stats-tool`, `survey-stats-tool`) can serve the new
indicator — a genuinely different data source. `LocalRegistryIndicatorQueryHandler` is the concrete
precedent: it wraps local services (`SurveyService`/`StakeholderService`) directly, no HTTP at all.

1. Implement `IndicatorQueryHandler` as a `@Service`/`@Component`, with a
   `public static final String HANDLER_KEY = "...";` constant — auto-picked up by
   `IndicatorQueryHandlerRegistry`, no manual registration:

   ```java
   public interface IndicatorQueryHandler {
       String handlerKey();
       IndicatorResult execute(IndicatorExecutionPlan plan);
   }
   ```

2. If the data lives in an external service, also implement `RemoteIndicatorClient`
   (`handler/RemoteIndicatorClient.java`) and read its javadoc caveat carefully: the declared
   `allowedAggregations`/dimensions on any `IndicatorDefinition` routed through this port must
   be kept in sync **by hand** with what the remote actually supports — there's no way to validate
   that automatically against a black-box remote service.

3. Two concrete worked examples, both implementing both interfaces, both delegating `execute()`
   straight to `query()`, both dispatching on `providerIndicatorKey()` via `switch` (see
   [Mental model](#mental-model-the-two-registries) above), and both keeping DSL-building (pure, no
   Spring dependency) fully separate from HTTP transport (wraps a `WebClient`) — this is what makes
   the query-builder unit-testable with zero network mocking:
   - `OpenAireStatsToolIndicatorQueryHandler` (`openaire/`) — serves 2 indicators from one
     handler, one external stats-tool with a filter-tree DSL (`OpenAireQueryBuilder` +
     `OpenAireStatsToolClient`).
   - `SurveyStatsToolIndicatorQueryHandler` (`survey/`) — serves 3 indicators from one handler, a
     *different* external stats-tool with a named-query DSL (`SurveyStatsToolQueryBuilder` +
     `SurveyStatsToolClient`, reusing `eu.openaire.observatory.statistics.StatsToolProperties` for
     config rather than duplicating a `@Value` endpoint string). Demonstrates two things the
     OpenAIRE handler doesn't: real server-side aggregation of a `MEASURE`
     (`oa.financial_investment` applies the requested `SUM`/`AVG`/`MIN`/`MAX` in Java, plus an
     explicit ×1,000,000 unit-scale correction) and an `ATTRIBUTE`/`RATIO` pair
     (`oa.country_initiative_status`/`oa.eu_country_coverage`) sharing one private data-access method
     (`fetchInitiativeStatusByCountry`) so the same underlying fact is never fetched twice.

## Known limitation: `country` doesn't support `NOT_IN`

`accessStatus` gained `NOT_IN` support (see `resolveNumeratorModes` in
`OpenAireStatsToolIndicatorQueryHandler`) alongside its existing `EQ`/`IN`/`NOT_EQ`. `country`
`NOT_IN` was investigated in the same pass and deliberately **not** shipped — not "not yet," but
likely not implementable at all with this codebase's OpenAIRE query DSL, for a structural reason
worth understanding before anyone tries again.

**What was tried.** The OpenAIRE stats-tool DSL supports leaf-level negation — a `groupFilters` block
with `"type":"!="` per value, combined with `"op":"AND"` — confirmed live for a *different* field/entity
(`Organizations.Country Code` on entity `Organizations`, where each row is one organization, so there's
no ambiguity). The natural next step was to reuse the same shape against
`result.organization.country.code` on entity `result`: `country NOT_IN [GR, DE]` →
`"country != GR AND country != DE"`.

Live-tested against the real endpoint, Europe-scoped publication counts for 2022:

| Query | Count | Expected (if `!=` were true exclusion) |
|---|---|---|
| Total | 2,822,018 | — |
| `country = GR` | 46,135 | — |
| `country != GR` | 2,791,271 | 2,775,883 (off by 15,388) |
| `country = GR OR country = DE` | 394,719 | — |
| `country != GR AND country != DE` | 2,554,494 | 2,427,299 (off by 127,195) |

Both mismatches point the same direction: `!=` is evaluated **existentially per organization**, not
universally per publication. A publication co-affiliated with GR and FR organizations satisfies
`country != GR` (via its FR affiliation) even though it *does* have a GR affiliation — so it leaks into
the "excluded" bucket instead of being excluded. Confirmed with two different JSON shapes for the same
logical query (one combined `groupFilters` block vs. two separate top-level blocks) — identical wrong
number both times, ruling out a query-construction mistake rather than a genuine DSL/data-model issue.

**Why the obvious fallback (enumerate the country list + complement) doesn't fix it either.** The first
instinct was: skip DSL-level negation, fetch the full country list from `CountryDimensionMemberProvider`,
subtract the excluded codes, and pass the remainder through the already-correct `=`/`OR` inclusion path
(`country IN {all except GR, DE}`). **This has the identical flaw.** `country IN [...]` is just as
existential as `country = X` — "has *an* org in this set" — so a publication with orgs {GR, FR} still
matches `country IN {FR, ...}` via its FR org, and leaks into the "not GR/DE" result despite having a
GR org too. Any approach built from flat per-leaf `AND`/`OR` conditions over a one-to-many field (a
publication can have organizations in several countries) can only express *existential* conditions
("has an org matching X"), never a *universal* one ("no org matches X") — that would need the DSL to
support something like `NOT EXISTS (...)`/subquery negation, which hasn't been confirmed to exist here.

**Decision.** `country` stays `EQ`/`IN` only on both real indicators. `EQ`/`IN` are genuinely correct as
shipped — inclusion is existential and *should* match co-affiliated publications, which is exactly the
right behavior for "OR". If `country` exclusion is revisited, it needs either confirmed DSL support for
real negation-of-an-existential (untested — would need to ask OpenAIRE or find it documented), or an
explicit, clearly-flagged decision to ship the "leaky" existential approximation anyway and document it
as a known inaccuracy for co-affiliated publications, rather than quietly shipping something that looks
like exclusion but isn't.

## Testing patterns

| Situation | Template |
|---|---|
| Remote/HTTP-backed handler | `openaire/OpenAireStatsToolIndicatorQueryHandlerTest` or `survey/SurveyStatsToolIndicatorQueryHandlerTest` — no live network: a real `WebClient` built with a stub `ExchangeFunction` that captures the outbound request and returns a canned response (shaped from real, live-confirmed data, not guessed). Asserts both the result values **and** the literal outbound request shape. |
| DSL-building logic in isolation | `openaire/OpenAireQueryBuilderTest` or `survey/SurveyStatsToolQueryBuilderTest` — pure, network-free assertions on the built request via Jackson. |
| Catalog-level structural checks (counts, bindings) | `IndicatorDefinitionCatalogTest`. |
| End-to-end validator wiring for a new catalog entry | `IndicatorDefinitionCatalogOpenAireWiringTest` or `IndicatorDefinitionCatalogSurveyStatsToolWiringTest`. |
| Reusable `IndicatorDefinition` fixtures for validator-focused tests | `IndicatorFixtures.java` (`countIndicator`, `ratioIndicator`, `nonTimeSeriesIndicator`, `requiredFilterIndicator`, `restrictedIndicator`) — use these instead of hand-rolling a definition when the test is about validator *behavior*, not about a specific catalog entry. |

None of these tests load a Spring context — handlers, the validator, and the catalog are all
constructed directly with `new`, which is why they run fast and don't need mocking frameworks beyond
a stub `ExchangeFunction` for HTTP.

## Checklist

- Every new indicator → touch `IndicatorCodes.java` (new constant) and `IndicatorDefinitionCatalog.java` (factory
  method, `allDefinitions()`) + its structural test — cheap regardless of what follows.
- Reusing existing dimensions → nothing further needed there. New dimension → + a new
  `DimensionMemberProvider` (+ a `DimensionDefinition` entry in the catalog).
- An existing handler's data source fits → + a new `case` and query method on
  `OpenAireStatsToolIndicatorQueryHandler` or `SurveyStatsToolIndicatorQueryHandler`. Doesn't fit
  → + a whole new `IndicatorQueryHandler` (+ `RemoteIndicatorClient` if it's remote-backed).
- Always → a validator-wiring test (`IndicatorDefinitionCatalogOpenAireWiringTest`/
  `IndicatorDefinitionCatalogSurveyStatsToolWiringTest`-style) for the new catalog entry's bindings, and a
  handler-level test following whichever template matches your handler's shape.
