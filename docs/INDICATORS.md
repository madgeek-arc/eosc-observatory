# Indicators API reference

Reference for the `/api/indicators`, `/api/dimensions`, and `/api/indicator-queries` endpoints —
the semantic indicator layer that lets a client discover queryable indicators and their dimensions,
then run a single generic query endpoint against any of them.


**Data sources**: the catalog has 9 indicators, all backed by real data, from 3 sources.
`publications.count` and `publications.access_status_share` call OpenAIRE's 
stats-tool (`https://services.openaire.eu/stats-tool/`) live, over the network, on every request.
`oa.financial_investment`, `oa.country_initiative_status`, `oa.eu_country_coverage`,
`repositories.long_term_preservation_count`, and `citizen_science.new_projects_count` call a
separate survey stats-tool (`stats-tool.endpoint`) the same way, scoped to survey years 2022+ — see
[`docs/INDICATOR_ROADMAP.md`](INDICATOR_ROADMAP.md) for the confirmed request/response shape and
data-coverage notes. `survey.countries_with_validated_answers` and
`stakeholder.participating_countries_count` are **local only** — no external HTTP call, backed
directly by this app's own registry (`SurveyService`/`StakeholderService`).

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/indicators` | List every indicator and its filter/group capabilities |
| `GET` | `/indicators/{code}` | Same, for one indicator |
| `GET` | `/dimensions/{code}/members?search=&limit=` | List a dimension's valid values (for building filter UIs) |
| `POST` | `/indicator-queries` | Run a query against one indicator |

## Core concepts

**Indicator shape** — every indicator has:
- `semanticType`: `MEASURE` (a summable quantity, e.g. a count), `RATIO` (numerator/denominator,
  never averaged directly), or `ATTRIBUTE` (non-numeric, e.g. a boolean status).
- `valueType`: `INTEGER`, `DECIMAL`, `BOOLEAN`, or `TEXT`.
- `unit`: `COUNT`, `PERCENT`, `CURRENCY`, `DAYS`, `HOURS`, `BYTES`, `SCORE`, or `NONE`.
- `renderHint`: `SCALAR` (a single number/series) or `ENTITY_MAP` (per-country boolean, for a map).

**Dimensions & bindings** — a dimension (e.g. `country`, `accessStatus`) is registered once and
referenced by any number of indicators. Each indicator's *binding* to a dimension declares:
- `usages`: which of `FILTER`, `GROUP`, `SORT`, `TIME` this indicator allows for that dimension.
- `allowedOperators`: which `FilterOperator`s this indicator accepts for it — may be **narrower**
  than the dimension's own `supportedOperators`.
- `requiredFilter`: if `true`, the query fails validation unless a filter on this dimension is given.

**`FilterOperator`** has 13 members (`EQ, NOT_EQ, IN, NOT_IN, GT, GTE, LT, LTE, BETWEEN, CONTAINS,
STARTS_WITH, IS_NULL, IS_NOT_NULL`), but only a few are actually wired up per indicator today — see
each indicator's dimension table below. Don't assume an operator works just because it's in the enum;
check the specific indicator's `allowedOperators`.

**Time series** — pass `timeSeries: {"from":2015,"to":2023}` (plain calendar years, inclusive) to get
one data point per year in range, each tagged with a `period` dimension. `from` and `to` must satisfy
`2015 <= from <= to <= <current year>` (else `InvalidTimeRangeException`). Omitting `timeSeries`
returns a single snapshot for the current calendar year, with no `period` key. `missingPeriods` is
optional and rarely needs setting — it falls back to the indicator's own default (`ZERO_FILL` for all
5 today).

**Comparison mode** — pass `comparisonMode: "TWO_POINT"` or `"CONSECUTIVE"` alongside `timeSeries` to
get period-over-period deltas instead of raw values. `TWO_POINT` diffs exactly `timeSeries.from` vs
`.to` (one row per group); `CONSECUTIVE` diffs every adjacent year across the range, e.g.
`from:2021,to:2024` yields 2021→2022, 2022→2023, 2023→2024 (one row per group per pair). Each row's
`value` is `{"absolute": ..., "percent": ...}`: for `MEASURE` indicators, `absolute` is the raw
difference and `percent` is relative change (`null` on a zero baseline); for `RATIO` indicators
(whose raw value is already a percentage), `absolute` is always `null` and `percent` is the
percentage-**point** difference instead. `ATTRIBUTE` indicators can't be diffed. Only indicators
whose handler actually fetches every year in a range support this — today that's `publications.count`
and `publications.access_status_share`; `oa.financial_investment` and the other survey-backed
indicators don't (see `TimeSeriesNotSupportedException`-adjacent `ComparisonNotSupportedException`
below). Response rows use `fromPeriod`/`toPeriod` keys instead of `period`.

**GroupBy / filter interaction** (same rule for every dimension): an `EQ` filter with no matching
`groupBy` entry aggregates over that one value into a single row; an `IN` filter aggregates over the
whole given subset into a single row; adding the dimension to `groupBy` instead returns one row per
value — the filtered subset if a filter was also given, else every member of that dimension's full
domain. Multiple `groupBy` dimensions cross as a cartesian product.

## Discovery endpoints

`GET /indicators` — list, `IndicatorCatalogItem[]` (note: `description` is **not** exposed here,
only `label`):

```json
{
  "items": [
    {
      "id": "publications.access_status_share",
      "label": "Publications by Access Status (Share)",
      "format": "percentage",
      "group": "Publications",
      "dimensions": [
        {
          "dimensionCode": "accessStatus",
          "label": "Access status",
          "type": "ENUM",
          "filterable": true,
          "groupable": false,
          "requiredFilter": true,
          "allowedOperators": ["EQ", "IN", "NOT_EQ"]
        },
        {
          "dimensionCode": "country",
          "label": "Country",
          "type": "ENTITY_REFERENCE",
          "filterable": true,
          "groupable": true,
          "requiredFilter": false,
          "allowedOperators": ["EQ"]
        }
      ],
      "supportsTimeRange": true
    }
  ]
}
```

`format` is `"percentage"` (unit `PERCENT`), `"map"` (`renderHint = ENTITY_MAP`), or `"number"`
(everything else). `GET /indicators/{code}` returns one such item directly (no `items` wrapper), or
`404` if the code doesn't exist.

`GET /dimensions/accessStatus/members` (fixed 4-value list, hardcoded — no backing storage):
```json
{"members":[
  {"code":"OA","label":"Open Access"},
  {"code":"CLOSED","label":"Closed Access"},
  {"code":"EMBARGOED","label":"Embargoed"},
  {"code":"RESTRICTED","label":"Restricted"}
]}
```

`GET /dimensions/country/members` — sourced from `Stakeholder.country` (real registered
stakeholders in this environment, not a fixed EU-27 list), e.g. `{"code":"GR","label":"Greece"}`.
Actual members depend on what's in the database.

`GET /dimensions/initiativeType/members` (fixed 4-value list): `OA_PUBLICATION_POLICY`,
`IMMEDIATE_OA_POLICY`, `FINANCIAL_STRATEGY`, `MONITORING_INITIATIVE`. Required as a filter on both
`oa.country_initiative_status` and `oa.eu_country_coverage` — see their reference sections below.

`GET /dimensions/surveyId/members` — sourced from this app's own `eosc-sb`-type survey `Model`s
(e.g. `{"code":"m-eosc-sb-2023","label":"Survey on National Contributions to EOSC and Open Science
2023"}`). Required as a filter on `survey.countries_with_validated_answers` — see its reference
section below.

## Query endpoint

`POST /indicator-queries`, body = `IndicatorQuery`:

| Field | Type | Required | Notes |
|---|---|---|---|
| `indicatorCode` | string | yes | e.g. `"publications.count"` |
| `aggregation` | string | no | Ignored for `RATIO` indicators |
| `filters` | `IndicatorFilter[]` | no | Defaults to `[]` if omitted |
| `groupBy` | string[] | no | Dimension codes; defaults to `[]` if omitted |
| `timeSeries` | object | no | `{"from","to","missingPeriods"}`; `from`/`to` are years, `2015 <= from <= to <= currentYear`; omit for a single snapshot |
| `comparisonMode` | string | no | `"TWO_POINT"` \| `"CONSECUTIVE"`; requires `timeSeries`; see "Comparison mode" above |
| `sort` | `SortRequest[]` | no | Defaults to `[]` if omitted |
| `limit` | integer | no | Defaults to 1000 server-side |

`IndicatorFilter`: `{"dimension": "...", "operator": "EQ", "values": ["..."]}` — `values` is always
an array, even for a single-value `EQ`.

Response = `IndicatorResult`:
```json
{
  "metadata": {"indicatorCode": "...", "label": "...", "valueType": "...", "unit": "...", "aggregation": "..."},
  "dimensions": ["country", "period"],
  "data": [
    {"dimensions": {"country": "GR", "period": "2023"}, "value": 30.1}
  ],
  "execution": {"generatedAt": "2026-08-11T...", "partial": false}
}
```
`data[].dimensions` only contains the keys listed in the top-level `dimensions` array — a key is
present only if that dimension was in `groupBy`, and `period` is present only when `timeSeries` was
set. When `comparisonMode` was set, `period` is replaced by `fromPeriod`/`toPeriod` and `value`
becomes `{"absolute": ..., "percent": ...}` — see "Comparison mode" above.

## Errors

Validation failures are plain `RuntimeException`s (`MissingRequiredFilterException`,
`UnsupportedDimensionException`, `UnsupportedOperatorException`, `TimeSeriesNotSupportedException`,
`InvalidTimeRangeException`, `ComparisonRequiresTimeSeriesException`, `InvalidComparisonRangeException`,
`ComparisonNotSupportedException`, `UnknownIndicatorException`, `InvalidAggregationException`,
`IndicatorAccessDeniedException`), each annotated with `@ResponseStatus` so it maps to the status
code in the table below (403/404/400 as applicable) with the exception's message as the response
body; anything else falls through to the generic handler and returns HTTP 500.

| Exception | When |
|---|---|
| `UnknownIndicatorException` | `indicatorCode` doesn't exist |
| `IndicatorAccessDeniedException` | Actor's access level is below the indicator's `accessLevel` |
| `InvalidAggregationException` | `aggregation` isn't in the indicator's `allowedAggregations` |
| `UnsupportedDimensionException` | Filtering/grouping by a dimension not bound to this indicator, or bound but not for that usage |
| `UnsupportedOperatorException` | Filter operator not in the dimension binding's `allowedOperators` |
| `MissingRequiredFilterException` | A `requiredFilter: true` dimension has no filter in the request |
| `TimeSeriesNotSupportedException` | `timeSeries` given but the indicator's `timePolicy` doesn't support it |
| `InvalidTimeRangeException` | `timeSeries.from`/`to` missing or outside `2015 <= from <= to <= currentYear` |
| `ComparisonRequiresTimeSeriesException` | `comparisonMode` given without `timeSeries` |
| `InvalidComparisonRangeException` | `comparisonMode` given but `timeSeries.from == timeSeries.to` |
| `ComparisonNotSupportedException` | `comparisonMode` given but the indicator is `ATTRIBUTE`-typed, or its handler doesn't fetch full ranges (e.g. survey-backed indicators) |

## Indicator reference

### `publications.count`

Real data (OpenAIRE). Number of published works. `MEASURE`, `INTEGER`, `COUNT`, `SCALAR`.

| Dimension | Usages | Operators | Required | Notes |
|---|---|---|---|---|
| `accessStatus` | FILTER, GROUP | EQ | no | Always an **exact single status** — never aggregates (unlike `access_status_share`) |
| `country` | FILTER, GROUP | EQ, IN | no | Absent = all-EU (continent=Europe scope, always applied). `IN` ORs multiple countries together. `NOT_IN` is **not** supported — see [Known limitation](../src/main/java/eu/openaire/observatory/indicator/README.md) |

```bash
# Total, current year
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"publications.count"}'

# One specific status
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"publications.count","filters":[{"dimension":"accessStatus","operator":"EQ","values":["EMBARGOED"]}]}'

# Multiple countries OR'd together
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"publications.count","filters":[{"dimension":"country","operator":"IN","values":["GR","DE"]}]}'

# Per-country map (native OpenAIRE groupBy, not a query-per-country loop)
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"publications.count","groupBy":["country"]}'

# Country x accessStatus x year range
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"publications.count","groupBy":["country","accessStatus"],"timeSeries":{"from":2022,"to":2023}}'
```

### `publications.access_status_share`

Real data (OpenAIRE). Percentage share of publications matching the given `accessStatus` filter, out
of **all** publications (the same total `publications.count` would return unfiltered). `RATIO`,
`DECIMAL`, `PERCENT`, `SCALAR`. `accessStatus` is **required** — there's no meaningful value without
specifying which status(es) to measure as a share of the total (it would trivially be 100%).

| Dimension | Usages | Operators | Required | Notes |
|---|---|---|---|---|
| `accessStatus` | FILTER | EQ, IN, NOT_EQ, NOT_IN | **yes** | See semantics below |
| `country` | FILTER, GROUP | EQ, IN | no | Absent = all-EU. `IN` ORs multiple countries together. `NOT_IN` is **not** supported — see [Known limitation](../src/main/java/eu/openaire/observatory/indicator/README.md) |

**`accessStatus` filter semantics** — worked example for a year with 30 OA / 50 Closed / 25
Embargoed / 15 Restricted = 120 total publications:

| Filter | Numerator | Value |
|---|---|---|
| `EQ ["OA"]` | Open Access only → 30 | `25.0%` |
| `EQ ["CLOSED"]` | Closed Access only → 50 | `41.7%` |
| `EQ ["EMBARGOED"]` | Embargoed only → 25 | `20.8%` |
| `IN ["CLOSED","EMBARGOED","RESTRICTED"]` | Explicit subset, OR'd → 90 | `75.0%` |
| `NOT_EQ ["OA"]` | Everything except OA → 90 | `75.0%` |
| `NOT_IN ["CLOSED","EMBARGOED"]` | Everything except those two → 45 (OA + Restricted) | `37.5%` |

`EQ`/`IN` return exactly the requested status(es); `NOT_EQ`/`NOT_IN` return everything **except** the
given value(s) — so `NOT_EQ ["OA"]` is the full non-open share, and any single non-open `EQ` value
returns *only* that status's own share, not an aggregate. `NOT_EQ` and `NOT_IN` behave identically here
(both complement over the same fixed 4-value status enum) — `NOT_IN` exists mainly so a client sending
several values to exclude can use the operator whose name matches its intent.

```bash
# Open access share
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"publications.access_status_share","filters":[{"dimension":"accessStatus","operator":"EQ","values":["OA"]}]}'

# Full non-open share
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"publications.access_status_share","filters":[{"dimension":"accessStatus","operator":"NOT_EQ","values":["OA"]}]}'

# Exclude two specific statuses
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"publications.access_status_share","filters":[{"dimension":"accessStatus","operator":"NOT_IN","values":["CLOSED","EMBARGOED"]}]}'

# Per-country map
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"publications.access_status_share","filters":[{"dimension":"accessStatus","operator":"EQ","values":["OA"]}],"groupBy":["country"]}'

# Time range
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"publications.access_status_share","filters":[{"dimension":"accessStatus","operator":"EQ","values":["OA"]}],"timeSeries":{"from":2021,"to":2024}}'
```

### `oa.financial_investment`

Real data (survey stats-tool, survey years 2022+ only). Reported financial investment in Open
Access publication (`Question56`). `MEASURE`, `DECIMAL`, `CURRENCY`, `SCALAR`. Survey values are
collected in millions of euros; the handler applies an explicit ×1,000,000 scale, so the returned
value is already in euros.

| Dimension | Usages | Operators | Required | Notes |
|---|---|---|---|---|
| `country` | FILTER, GROUP | EQ, IN | no | Absent + no `groupBy` aggregates every country with an answer per the requested `aggregation` (`SUM` by default; `AVG`/`MIN`/`MAX` also allowed) |

```bash
# Total across all countries with an answer (SUM by default)
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"oa.financial_investment"}'

# Average per country
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"oa.financial_investment","aggregation":"AVG"}'

# Per-country map
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"oa.financial_investment","groupBy":["country"]}'
```

### `oa.country_initiative_status`

Real data (survey stats-tool, survey years 2022+ only). Per-country Yes/No adoption status of a
national OA initiative, for one `initiativeType` at a time (see the question-code table in
[`docs/INDICATOR_ROADMAP.md`](INDICATOR_ROADMAP.md)). `ATTRIBUTE`, `BOOLEAN`, `NONE`, `ENTITY_MAP`.
Always returns one row per answering country — `country` grouping is inherent to this indicator's
meaning, so there is no `country` dimension binding to request it through.

| Dimension | Usages | Operators | Required | Notes |
|---|---|---|---|---|
| `initiativeType` | FILTER | EQ | **yes** | Picks which national initiative's Yes/No status to report |

```bash
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"oa.country_initiative_status","filters":[{"dimension":"initiativeType","operator":"EQ","values":["OA_PUBLICATION_POLICY"]}]}'
```

### `oa.eu_country_coverage`

Real data (survey stats-tool, survey years 2022+ only). Percentage of that year's survey respondents
with a Yes on the given `initiativeType`'s national initiative question, out of all respondents to
that question that year — the same underlying per-country fact as `oa.country_initiative_status`,
turned into a percentage instead of a per-country map; a country with no answer to this specific
question still counts toward the denominator, it just isn't counted as a Yes. Despite the name, this
is **not** filtered to EU membership — the denominator matches `observatory-ui`'s own
`calculatePercentage` exactly (that year's raw respondent row count), which is not itself EU-scoped;
see `docs/INDICATOR_ROADMAP.md` for why this inherits the legacy system's own scope rather than
correcting it. `RATIO`, `DECIMAL`, `PERCENT`, `SCALAR`. Intrinsically aggregate-only — unlike
`publications.access_status_share`, there is no `country` binding for a per-country drill-down.

| Dimension | Usages | Operators | Required | Notes |
|---|---|---|---|---|
| `initiativeType` | FILTER | EQ | **yes** | Picks which national initiative's coverage to report |

```bash
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"oa.eu_country_coverage","filters":[{"dimension":"initiativeType","operator":"EQ","values":["MONITORING_INITIATIVE"]}]}'
```

### `repositories.long_term_preservation_count`

Real data (survey stats-tool, survey years 2022+ only). Number of repositories offering long-term
data preservation, per country (survey `Question89`). `MEASURE`, `INTEGER`, `COUNT`, `SCALAR`.

| Dimension | Usages | Operators | Required | Notes |
|---|---|---|---|---|
| `country` | FILTER, GROUP | EQ, IN | no | Absent + no `groupBy` aggregates every country with an answer per the requested `aggregation` (`SUM` by default; `AVG`/`MIN`/`MAX` also allowed) |

```bash
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"repositories.long_term_preservation_count"}'
```

### `citizen_science.new_projects_count`

Real data (survey stats-tool, survey years 2022+ only). Number of new projects with a citizen
science dimension started, per country (survey `Question101`). Same shape as
`repositories.long_term_preservation_count`. `MEASURE`, `INTEGER`, `COUNT`, `SCALAR`.

| Dimension | Usages | Operators | Required | Notes |
|---|---|---|---|---|
| `country` | FILTER, GROUP | EQ, IN | no | Absent + no `groupBy` aggregates every country with an answer per the requested `aggregation` (`SUM` by default; `AVG`/`MIN`/`MAX` also allowed) |

```bash
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"citizen_science.new_projects_count","groupBy":["country"]}'
```

### `survey.countries_with_validated_answers`

Real data (local — no external HTTP call, backed directly by
`SurveyServiceImpl.getCountriesWithValidatedAnswer`). Number of distinct countries with at least
one validated answer for the given survey. `MEASURE`, `INTEGER`, `COUNT`, `SCALAR`. No time series
support (`supportsTimeRange: false`).

| Dimension | Usages | Operators | Required | Notes |
|---|---|---|---|---|
| `surveyId` | FILTER | EQ | **yes** | A real survey id, e.g. `"m-eosc-sb-2023"` — see `GET /dimensions/surveyId/members` |

```bash
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"survey.countries_with_validated_answers","filters":[{"dimension":"surveyId","operator":"EQ","values":["m-eosc-sb-2023"]}]}'
```

### `stakeholder.participating_countries_count`

Real data (local — no external HTTP call, backed directly by `StakeholderService.getWithFilter("type",
"eosc-sb")`). Number of distinct countries with an EOSC Steering Board stakeholder registered.
`MEASURE`, `INTEGER`, `COUNT`, `SCALAR`. No time series support (`supportsTimeRange: false`), no
dimension bindings at all — the stakeholder type is fixed to `eosc-sb`, not client-filterable.

```bash
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"stakeholder.participating_countries_count"}'
```

## Live network dependency

`publications.count` and `publications.access_status_share` call
`https://services.openaire.eu/stats-tool/` (`openaire.stats-tool.endpoint` in
`application.properties`) synchronously, on every request, with no caching. `oa.financial_investment`,
`oa.country_initiative_status`, `oa.eu_country_coverage`, `repositories.long_term_preservation_count`,
and `citizen_science.new_projects_count` call a second, unrelated live upstream the same way
(`stats-tool.endpoint`). A slow or unavailable upstream means slow or failing requests for every
indicator that depends on it — the two upstreams fail independently of each other.
`survey.countries_with_validated_answers` and `stakeholder.participating_countries_count` make no
external HTTP call at all — they query this app's own registry directly, so neither upstream
outage affects them.
