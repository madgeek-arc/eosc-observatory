# Indicators API reference

Reference for the `/api/indicators`, `/api/dimensions`, and `/api/indicator-queries` endpoints —
the semantic indicator layer that lets a client discover queryable indicators and their dimensions,
then run a single generic query endpoint against any of them.


**Data sources**: the catalog has 13 indicators, all backed by real data, from 3 sources.
`publications.count`, `publications.access_status_share`, `datasets.count`, `datasets.access_status_share`,
`software.count`, and `publications.oa_license_status_count` call OpenAIRE's
stats-tool (`https://services.openaire.eu/stats-tool/`) live, over the network, on every request, and
support real `timeSeries` (one point per year in the requested range). `oa.financial_investment`,
`oa.country_initiative_status`, `oa.eu_country_coverage`, `repositories.long_term_preservation_count`,
and `citizen_science.new_projects_count` call a separate survey stats-tool (`stats-tool.endpoint`) the
same way — despite the `oa.`/`repositories.`/`citizen_science.` prefixes, `oa.financial_investment`
(via a required `investmentArea` filter) and `oa.country_initiative_status`/`oa.eu_country_coverage`
(via a required `initiativeType` filter) are cross-topic, also covering Open Data and FAIR Data.
`oa.financial_investment`, `repositories.long_term_preservation_count`, and
`citizen_science.new_projects_count` support real `timeSeries` (`supportsTimeRange: true`); a
`surveyYear` filter is still accepted as a shorthand for a single-year, non-series query.
`oa.country_initiative_status` and `oa.eu_country_coverage` don't support `timeSeries` at all
(`supportsTimeRange: false`) — each query fetches exactly one survey year, picked via a required
`surveyYear` filter (2021 used incompatible question numbering upstream, so no year before 2022 is
available). `oa.country_initiative_status` returns one of 3 values per country — `YES`, `NO`, or
`AWAITING_DATA` for a country that hasn't answered yet — not a plain boolean.
`survey.countries_with_validated_answers` and `stakeholder.participating_countries_count` are
**local only** — no external HTTP call, backed directly by this app's own registry
(`SurveyService`/`StakeholderService`).

## Legacy indicator mapping

The catalog's OpenAIRE/survey-backed indicators replace 8 real legacy indicator definitions (ids
67–74). This is a many-to-one mapping in places — several legacy ids collapse into one new
indicator, disambiguated by a filter (e.g. `initiativeType`) instead of a separate indicator code:

| Legacy id(s) | Legacy name | New indicator | Notes |
|---|---|---|---|
| 67 | OA Publication in Europe (OA vs Closed) | `publications.access_status_share` | Intrinsically scoped to Europe |
| 68, 70, 71, 72 | EU Countries with Policy / Immediate-OA-Policy / Financial-Strategy / Monitoring on OA Publication | `oa.eu_country_coverage` | Intrinsically scoped to EU member states; disambiguated per legacy id via the `initiativeType` filter |
| 69 | Financial investments in OA Publication | `oa.financial_investment` | |
| 73, 74 | National policy on Open Access publications / National monitoring on Open Access publications | `oa.country_initiative_status` | Originally `format: map` (now `renderHint: ENTITY_MAP`); per-country grouping is mandatory, not optional |

Verified end-to-end against the legacy UI's actual 2023 values (not just structurally) via
`POST /indicator-queries` against a locally-run instance, compared row-for-row against the same
external endpoints `observatory-ui` calls — all 8 legacy ids matched. Id 67's match required a
denominator fix; see the note under `publications.access_status_share` below.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/indicators/definitions` | List every generic indicator and its filter/group capabilities |
| `GET` | `/indicators/definitions/{code}` | Same, for one indicator |
| `GET` | `/indicators/pre-defined` | List every pre-defined dashboard widget (flat, one row per widget) |
| `GET` | `/indicators/pre-defined/{id}` | Same, for one widget |
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
whose handler actually fetches every year in a range support this — today that's the 5
OpenAIRE-backed ones (`publications.count`, `publications.access_status_share`, `datasets.count`,
`software.count`, `publications.oa_license_status_count`). The 5 survey-backed indicators
(`oa.financial_investment` and the rest) don't support `timeSeries` at all
(`supportsTimeRange: false`), so `comparisonMode` fails the same way — `TimeSeriesNotSupportedException`
fires before `comparisonMode` is even considered, not `ComparisonNotSupportedException` (that one's
for time-series-capable indicators whose semantic type or handler still can't be diffed, e.g.
`ATTRIBUTE` indicators). Response rows use `fromPeriod`/`toPeriod` keys instead of `period`.

**GroupBy / filter interaction** (same rule for every dimension): an `EQ` filter with no matching
`groupBy` entry aggregates over that one value into a single row; an `IN` filter aggregates over the
whole given subset into a single row; adding the dimension to `groupBy` instead returns one row per
value — the filtered subset if a filter was also given, else every member of that dimension's full
domain. Multiple `groupBy` dimensions cross as a cartesian product.

## Discovery endpoints

`GET /indicators/definitions` — list, `IndicatorCatalogItem[]` (note: `description` is **not** exposed
here, only `label`):

```json
{
  "items": [
    {
      "id": "publications.access_status_share",
      "label": "Publications by Access Status (Share)",
      "format": "percentage",
      "renderHint": "SCALAR",
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

`format` and `renderHint` are two orthogonal axes, each authored once on the indicator: `format` is
how to format the value (`"percentage"`, `"number"`, `"currency"`, `"category"`, or `"yes-no"`);
`renderHint` is `"SCALAR"` (a single number/series) or `"ENTITY_MAP"` (per-country breakdown).
`GET /indicators/definitions/{code}` returns one such item directly (no `items` wrapper), or `404`
if the code doesn't exist.

### Pre-defined dashboard widgets

`GET /indicators/pre-defined` — a flat, one-row-per-widget list for a page (like Explore) that wants
to iterate a fixed set of dashboard cards directly, rather than build queries against the generic
catalog above. Each row is a concrete instance of one of the generic indicators above, plus whatever
filter value disambiguates it — e.g. `oa.country_initiative_status` alone serves close to a dozen
different "National Monitoring on X" widgets here, one row per topic area. `GET
/indicators/pre-defined/{id}` returns one such item directly, or `404` if the id doesn't exist.

```json
{
  "results": [
    {
      "id": "od-4",
      "label": "National Policy on Open Data",
      "group": "Open Data",
      "chartType": "POLICY",
      "format": "yes-no",
      "source": "survey-stats-tool",
      "allowedSeriesAggregations": [],
      "supportsTrend": false,
      "supportsMap": true,
      "indicatorCode": "oa.country_policy_status",
      "presetFilters": {"initiativeType": ["OPEN_DATA_POLICY"]}
    },
    {
      "id": "oa-1",
      "label": "OA Publication in Europe (OA vs Closed)",
      "group": "Open Access Publications",
      "chartType": "SCALAR",
      "format": "percentage",
      "source": "openaire-stats-tool",
      "allowedSeriesAggregations": ["AVG", "MEDIAN"],
      "supportsTrend": true,
      "supportsMap": false,
      "indicatorCode": "publications.access_status_share",
      "presetFilters": {"accessStatus": ["OA"]}
    }
  ]
}
```

- `chartType` — how to render the widget: `"SCALAR"` (single aggregate value/snapshot), `"LINE"`
  (time series), `"MAP"` (binary/tri-state per-country breakdown), or `"POLICY"` (4-state per-country
  policy breakdown — Mandatory/Non-mandatory/No-policy/Awaiting-data). The same enum used by
  `DashboardItem.chartType`, so a client can copy this value straight onto a dashboard item built
  from this widget. This is only the widget's *default* view — `supportsTrend`/`supportsMap` below
  tell a client what else is available; which one actually gets saved onto `DashboardItem.chartType`
  is entirely the UI's choice, not something the backend picks.
- `format` — derived from the widget's underlying indicator (`IndicatorDefinition.format`), same
  vocabulary as `IndicatorCatalogItem.format`: `"percentage"`, `"number"`, `"currency"`,
  `"category"` (multi-value per-country status, e.g. a policy's Mandatory/Non-mandatory/No-policy
  breakdown), or `"yes-no"` (binary/3-state per-country status).
- `source` — **not** a legacy question code. It's the handler that actually answers the widget's
  underlying indicator: `"openaire-stats-tool"`, `"survey-stats-tool"`, or `"local-registry"` —
  the same local-vs-proxied distinction documented under "Data sources" above, derived from the
  linked indicator's own execution binding so it can't drift out of sync with which handler is
  really wired up.
- `allowedSeriesAggregations` — the exact `AggregationType` values meaningful for this widget's
  query (e.g. `["AVG", "MEDIAN"]`, or `[]` for none); a curated list on the widget, not derived from
  the indicator's `AggregationPolicy`. Same type as `IndicatorQuery.seriesAggregations`, so a client
  can copy it straight across when building the query.
- `supportsTrend` — whether this widget's underlying indicator supports time series, derived from
  `IndicatorDefinition.timePolicy().supported()` (the same fact `IndicatorCatalogItem` exposes as
  `supportsTimeRange`). A capability flag only — it tells the UI whether adding a `timeSeries` request
  to the `IndicatorQuery` makes sense; it doesn't imply any particular chart type.
- `supportsMap` — whether this widget's underlying indicator is a per-country breakdown, derived from
  `IndicatorDefinition.renderHint() == ENTITY_MAP`. Also a capability flag only — the UI decides
  whether to actually render it as a map.
- `indicatorCode`/`presetFilters` — the same values `toIndicatorQuery` merges into a request
  server-side; exposed so a client can build its own `IndicatorQuery` (and from it, a
  `DashboardItem`) to save via `PUT /dashboard`, without a dedicated "add widget to dashboard"
  endpoint.
- `id` — mostly `{topic}-{n}` (e.g. `oa-1`), sourced from the original indicator inventory. A handful
  carry a `-x` suffix (e.g. `oa-8-x`) — these 8 "National Financial Strategy on X" widgets aren't in
  the source inventory; they were added for symmetry with the Policy/Monitoring pairs every topic
  already has (see the `-x` id as the marker that this widget is a catalog addition, not an
  inventory-backed one).

`POST /indicators/pre-defined/{id}/query` — actually runs a widget. Body is a plain `IndicatorQuery`
(same shape as `POST /indicator-queries` above) with `indicatorCode` **ignored** — the widget's own
`indicatorCode` and preset filter(s) are always used, resolved from `{id}`. The request's own `filters`
are appended after the preset ones; everything else (`aggregation`, `groupBy`, `timeSeries`,
`comparisonMode`, `sort`, `limit`, `seriesAggregations`) passes straight through. `404` if `{id}`
doesn't exist. Response is a plain `IndicatorResult`, identical to `/indicator-queries`.

```bash
curl -X POST localhost:8280/api/indicators/pre-defined/oa-1/query \
  -d '{"filters":[{"dimension":"surveyYear","operator":"EQ","values":["2023"]}]}'
```
`oa-1` and `oa-2` ("OA Publication in Europe" / "Trend of Open Access Publications") share the same
`indicatorCode`+preset filter — running `oa-2` instead is the same request with `"timeSeries":
{"from":2022,"to":2023}` in place of the plain filter, since the only difference between the two
widgets is whether the caller asks for a time series.

If `filters` names a dimension the widget already presets (e.g. `accessStatus` against `oa-1`, which
*is* `accessStatus=OA`), the request 400s with `PresetFilterConflictException` rather than silently
unioning or overriding the preset value — see the errors table below.

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

`GET /dimensions/surveyYear/members` (fixed list, hardcoded — no backing storage): `2022`, `2023`.
Required as a filter on all 5 survey-stats-tool-backed indicators (`oa.financial_investment`,
`oa.country_initiative_status`, `oa.eu_country_coverage`, `repositories.long_term_preservation_count`,
`citizen_science.new_projects_count`) — see their reference sections below.

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
| `seriesAggregations` | `AggregationType[]` | no | Defaults to `[]`. Only meaningful alongside `timeSeries`: one or more summary statistics (e.g. `AVG`, `MEDIAN`) computed across the *whole* queried range, independent of `aggregation` — see the `summary` field below. `NONE` is always rejected here, even for indicators whose policy allows it as the primary `aggregation` |

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
  "execution": {"generatedAt": "2026-08-11T...", "partial": false},
  "summary": []
}
```
`data[].dimensions` only contains the keys listed in the top-level `dimensions` array — a key is
present only if that dimension was in `groupBy`, and `period` is present only when `timeSeries` was
set. When `comparisonMode` was set, `period` is replaced by `fromPeriod`/`toPeriod` and `value`
becomes `{"absolute": ..., "percent": ...}` — see "Comparison mode" above.

`summary` is `[]` unless the request set `seriesAggregations` alongside `timeSeries`. When populated,
it carries one row per (non-period-dimension group × requested aggregation type) — e.g. querying a
plain (ungrouped) series with `seriesAggregations: ["AVG", "MEDIAN"]` returns exactly 2 rows:
```json
"summary": [
  {"dimensions": {"aggregation": "AVG"}, "value": 1115.0},
  {"dimensions": {"aggregation": "MEDIAN"}, "value": 1230}
]
```
If the query was also grouped (e.g. `groupBy: ["country"]`), each group gets its own AVG/MEDIAN row
(`{"dimensions": {"country": "GR", "aggregation": "AVG"}, ...}`), computed from only that group's
periods — never blended across groups. `data` is always returned untouched alongside `summary`; the
two are independent, additive views of the same series.

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
| `PresetFilterConflictException` | `POST /indicators/pre-defined/{id}/query` only: request `filters` name a dimension the widget already presets |

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
of the sum of all 4 known access-mode values (`OA`+`CLOSED`+`EMBARGOED`+`RESTRICTED`) — **not** the
same total `publications.count` returns unfiltered, which is ~21% larger because it also includes
publications with no recognized access-mode value; the two totals don't reconcile, so
`percent × publications.count(unfiltered) ≠ numerator`. `RATIO`, `DECIMAL`, `PERCENT`, `SCALAR`.
`accessStatus` is **required** — there's no meaningful value without specifying which status(es) to
measure as a share of the total (it would trivially be 100%).

Verified live against the legacy UI's own definition of "total" (2023 data, legacy id 67): an
earlier version used an unrestricted count for the denominator instead of the 4-value sum and
understated the real share by ~12 percentage points (58.8% vs. the correct 71%) — fixed by summing
all 4 `OpenAireAccessMode` values for both denominator sub-queries.

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

### `datasets.access_status_share`

Real data (OpenAIRE). Open Data page counterpart to `publications.access_status_share` — identical
semantics and query shape, just `entity=dataset` instead of `entity=publication`. `RATIO`, `DECIMAL`,
`PERCENT`, `SCALAR`. `accessStatus` is **required**, same as `publications.access_status_share`.

| Dimension | Usages | Operators | Required | Notes |
|---|---|---|---|---|
| `accessStatus` | FILTER | EQ, IN, NOT_EQ, NOT_IN | **yes** | Same semantics as `publications.access_status_share` |
| `country` | FILTER, GROUP | EQ, IN | no | Absent = all-EU |

```bash
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"datasets.access_status_share","filters":[{"dimension":"accessStatus","operator":"EQ","values":["OA"]}]}'
```

### `oa.financial_investment`

Real data (survey stats-tool, survey years 2022+ only). Reported financial investment in the topic
area picked by the required `investmentArea` filter (survey `Question56`/`Question68`/`Question64`
for `OA_PUBLICATION`/`OPEN_DATA`/`FAIR_DATA` — see `GET /dimensions/investmentArea/members`).
`MEASURE`, `DECIMAL`, `CURRENCY`, `SCALAR`. Survey values are collected in millions of euros; the
handler applies an explicit ×1,000,000 scale, so the returned value is already in euros. Despite the
`oa.` prefix this is cross-topic, not OA-Publication-only. Supports a real `timeSeries` (one point
per year); without one, the optional `surveyYear` filter picks a single year.

| Dimension | Usages | Operators | Required | Notes |
|---|---|---|---|---|
| `investmentArea` | FILTER | EQ | **yes** | Picks which topic area's investment to report — see `GET /dimensions/investmentArea/members` |
| `surveyYear` | FILTER | EQ | no | A single survey year, e.g. `"2023"` — alternative to `timeSeries` |
| `country` | FILTER, GROUP | EQ, IN | no | Absent + no `groupBy` aggregates every country with an answer per the requested `aggregation` (`SUM` by default; `AVG`/`MEDIAN`/`MIN`/`MAX` also allowed) |

```bash
# Total across all countries with an answer (SUM by default)
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"oa.financial_investment","filters":[{"dimension":"investmentArea","operator":"EQ","values":["OA_PUBLICATION"]},{"dimension":"surveyYear","operator":"EQ","values":["2023"]}]}'

# Open Data investment, average per country
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"oa.financial_investment","filters":[{"dimension":"investmentArea","operator":"EQ","values":["OPEN_DATA"]},{"dimension":"surveyYear","operator":"EQ","values":["2023"]}],"aggregation":"AVG"}'

# FAIR Data investment, per-country map
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"oa.financial_investment","filters":[{"dimension":"investmentArea","operator":"EQ","values":["FAIR_DATA"]},{"dimension":"surveyYear","operator":"EQ","values":["2023"]}],"groupBy":["country"]}'

# Trend across years (2022+)
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"oa.financial_investment","filters":[{"dimension":"investmentArea","operator":"EQ","values":["OA_PUBLICATION"]}],"timeSeries":{"from":2022,"to":2023}}'
```

### `oa.country_initiative_status`

Real data (survey stats-tool, survey years 2022+ only). Per-country adoption status of a national
initiative, for one `initiativeType` at a time — despite the `oa.` prefix and code names, this is
cross-topic: OA Publication, Open Data, and FAIR Data initiative types are all supported (see
`GET /dimensions/initiativeType/members`). `ATTRIBUTE`, `TEXT`, `NONE`, `ENTITY_MAP`. Returns one of
3 values per answering-or-pending country — `YES`, `NO`, or `AWAITING_DATA` for a country that
hasn't answered yet — not a plain boolean; the underlying survey question is a flat Yes/No radio
field with no richer status, so a 4-state Mandatory/Policy-but-not-mandatory/No-policy/Awaiting-data
model isn't derivable from this data. Always returns one row per country — `country` grouping is
inherent to this indicator's meaning, so there is no `country` dimension binding to request it
through. No time series support (`supportsTimeRange: false`) — each query fetches exactly one survey
year, picked via the required `surveyYear` filter.

| Dimension | Usages | Operators | Required | Notes |
|---|---|---|---|---|
| `initiativeType` | FILTER | EQ | **yes** | Picks which national initiative's status to report — see `GET /dimensions/initiativeType/members` |
| `surveyYear` | FILTER | EQ | **yes** | The survey year to fetch, e.g. `"2023"` — see `GET /dimensions/surveyYear/members` |

```bash
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"oa.country_initiative_status","filters":[{"dimension":"initiativeType","operator":"EQ","values":["OA_PUBLICATION_POLICY"]},{"dimension":"surveyYear","operator":"EQ","values":["2023"]}]}'

# Open Data monitoring status per country
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"oa.country_initiative_status","filters":[{"dimension":"initiativeType","operator":"EQ","values":["OPEN_DATA_MONITORING_INITIATIVE"]},{"dimension":"surveyYear","operator":"EQ","values":["2023"]}]}'
```

### `oa.eu_country_coverage`

Real data (survey stats-tool, survey years 2022+ only). Percentage of that year's survey respondents
with a Yes on the given `initiativeType`'s national initiative question, out of all respondents to
that question that year — the same underlying per-country fact as `oa.country_initiative_status`
(same cross-topic `initiativeType` vocabulary), turned into a percentage instead of a per-country
map; a country with no answer to this specific question still counts toward the denominator, it just
isn't counted as a Yes. Despite the name, this is **not** filtered to EU membership — the denominator
matches `observatory-ui`'s own `calculatePercentage` exactly (that year's raw respondent row count,
including non-EU respondents — e.g. CH/NO/RS/TR/GE/AM/FO were observed live for 2023), inheriting the
legacy system's own (non-EU-scoped) definition rather than correcting it. `RATIO`, `DECIMAL`,
`PERCENT`, `SCALAR`. Intrinsically aggregate-only — unlike `publications.access_status_share`, there
is no `country` binding for a per-country drill-down. No time series support
(`supportsTimeRange: false`) — each query fetches exactly one survey year, picked via the required
`surveyYear` filter.

| Dimension | Usages | Operators | Required | Notes |
|---|---|---|---|---|
| `initiativeType` | FILTER | EQ | **yes** | Picks which national initiative's coverage to report — see `GET /dimensions/initiativeType/members` |
| `surveyYear` | FILTER | EQ | **yes** | The survey year to fetch, e.g. `"2023"` — see `GET /dimensions/surveyYear/members` |

```bash
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"oa.eu_country_coverage","filters":[{"dimension":"initiativeType","operator":"EQ","values":["MONITORING_INITIATIVE"]},{"dimension":"surveyYear","operator":"EQ","values":["2023"]}]}'

# FAIR Data policy coverage
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"oa.eu_country_coverage","filters":[{"dimension":"initiativeType","operator":"EQ","values":["FAIR_DATA_POLICY"]},{"dimension":"surveyYear","operator":"EQ","values":["2023"]}]}'
```

### `repositories.long_term_preservation_count`

Real data (survey stats-tool, survey years 2022+ only). Number of repositories offering long-term
data preservation, per country (survey `Question89`). `MEASURE`, `INTEGER`, `COUNT`, `SCALAR`.
Supports a real `timeSeries` (one point per year); without one, the optional `surveyYear` filter
picks a single year.

| Dimension | Usages | Operators | Required | Notes |
|---|---|---|---|---|
| `surveyYear` | FILTER | EQ | no | A single survey year, e.g. `"2023"` — alternative to `timeSeries` |
| `country` | FILTER, GROUP | EQ, IN | no | Absent + no `groupBy` aggregates every country with an answer per the requested `aggregation` (`SUM` by default; `AVG`/`MEDIAN`/`MIN`/`MAX` also allowed) |

```bash
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"repositories.long_term_preservation_count","filters":[{"dimension":"surveyYear","operator":"EQ","values":["2023"]}]}'
```

### `citizen_science.new_projects_count`

Real data (survey stats-tool, survey years 2022+ only). Number of new projects with a citizen
science dimension started, per country (survey `Question101`). Same shape as
`repositories.long_term_preservation_count`. `MEASURE`, `INTEGER`, `COUNT`, `SCALAR`. Supports a real
`timeSeries` (one point per year); without one, the optional `surveyYear` filter picks a single year.

| Dimension | Usages | Operators | Required | Notes |
|---|---|---|---|---|
| `surveyYear` | FILTER | EQ | no | A single survey year, e.g. `"2023"` — alternative to `timeSeries` |
| `country` | FILTER, GROUP | EQ, IN | no | Absent + no `groupBy` aggregates every country with an answer per the requested `aggregation` (`SUM` by default; `AVG`/`MEDIAN`/`MIN`/`MAX` also allowed) |

```bash
curl -X POST localhost:8280/api/indicator-queries -H 'Content-Type: application/json' \
  -d '{"indicatorCode":"citizen_science.new_projects_count","filters":[{"dimension":"surveyYear","operator":"EQ","values":["2023"]}],"groupBy":["country"]}'
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

## Access control

Every indicator in the catalog ships as `IndicatorAccessLevel.PUBLIC` today, including
`oa.country_initiative_status`, `oa.eu_country_coverage`, and `oa.financial_investment`, whose
underlying survey questions (`Question6`/`Question7`) are RESTRICTED-tier when accessed through
`StatsToolWrapperController`'s own ACL. This is a deliberate decision, not an oversight: the
indicator layer's own authorization seam (`IndicatorAuthorizationService`/`ActorContext`) is a
real, production-wired no-op today (`AllowAllIndicatorAuthorizationService`), so no indicator is
currently subject to real per-actor access restriction regardless of its declared `accessLevel`.
