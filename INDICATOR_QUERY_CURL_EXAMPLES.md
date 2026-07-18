# `/api/indicator-queries` curl examples

**Note**: superseded the flat legacy-id (`"67"`–`"74"`) contract from an earlier iteration. The
mock catalog now exposes the 5 real collapsed definitions from `LegacyIndicatorCatalogMappingTest`
(browse `GET /api/indicators` to discover codes and their filterable/groupable dimensions — there
is no fixed id list for the client to hardcode).

All examples assume the default local port, `server.port=8280` (`src/main/resources/application.properties`) —
`server.servlet.context-path=/api` is already folded into the paths below.

`POST /api/indicator-queries`, body = `IndicatorQuery`:

```java
public record IndicatorQuery(
    String indicatorCode,
    AggregationType aggregation,      // null for RATIO indicators
    List<IndicatorFilter> filters,
    List<String> groupBy,
    TimeSeriesRequest timeSeries,     // null = no time series requested
    List<SortRequest> sort,
    Integer limit
)
```

Values here are synthetic (`MockIndicatorQueryHandler` — deterministic pseudo-random, not real
data). `unit: COUNT`/`CURRENCY` fields are always whole numbers; `unit: PERCENT` fields always have
1 decimal place; `renderHint: ENTITY_MAP` fields are booleans.

## Single country, single year (`oa.financial_investment`)

```bash
curl -X POST localhost:8280/api/indicator-queries \
  -H "Content-Type: application/json" \
  -d '{
    "indicatorCode": "oa.financial_investment",
    "filters": [
      { "dimension": "country", "operator": "EQ", "values": ["GR"] }
    ],
    "groupBy": [],
    "timeSeries": { "grain": "YEAR", "from": "2023-01-01", "to": "2023-01-01" },
    "sort": [],
    "limit": null
  }'
```

## Multiple countries, multiple years, map format (`oa.country_initiative_status`)

`initiativeType` is a **required** filter on this definition — omitting it fails validation with
`MissingRequiredFilterException`.

```bash
curl -X POST localhost:8280/api/indicator-queries \
  -H "Content-Type: application/json" \
  -d '{
    "indicatorCode": "oa.country_initiative_status",
    "filters": [
      { "dimension": "initiativeType", "operator": "EQ", "values": ["OA_PUBLICATION_POLICY"] }
    ],
    "groupBy": ["country"],
    "timeSeries": { "grain": "YEAR", "from": "2021-01-01", "to": "2023-01-01" },
    "sort": [],
    "limit": null
  }'
```

## EU-wide coverage ratio, also requires `initiativeType` (`oa.eu_country_coverage`)

```bash
curl -X POST localhost:8280/api/indicator-queries \
  -H "Content-Type: application/json" \
  -d '{
    "indicatorCode": "oa.eu_country_coverage",
    "filters": [
      { "dimension": "initiativeType", "operator": "EQ", "values": ["MONITORING_INITIATIVE"] }
    ],
    "groupBy": [],
    "timeSeries": { "grain": "YEAR", "from": "2023-01-01", "to": "2023-01-01" },
    "sort": [],
    "limit": null
  }'
```

## Europe-wide, no country dimension (`publications.oa_share`)

```bash
curl -X POST localhost:8280/api/indicator-queries \
  -H "Content-Type: application/json" \
  -d '{
    "indicatorCode": "publications.oa_share",
    "filters": [],
    "groupBy": [],
    "timeSeries": { "grain": "YEAR", "from": "2023-01-01", "to": "2023-01-01" },
    "sort": [],
    "limit": null
  }'
```

## Grouping by a non-country dimension (`publications.count` by `accessStatus`)

`groupBy` isn't country-only — any dimension the definition declares `GROUP`-able works the same
way. With no matching filter, every member of that dimension's domain is returned (here, all 4
`accessStatus` values from `GET /api/dimensions/accessStatus/members`).

```bash
curl -X POST localhost:8280/api/indicator-queries \
  -H "Content-Type: application/json" \
  -d '{
    "indicatorCode": "publications.count",
    "filters": [],
    "groupBy": ["accessStatus"],
    "timeSeries": { "grain": "YEAR", "from": "2023-01-01", "to": "2023-01-01" },
    "sort": [],
    "limit": null
  }'
```

Multiple `groupBy` dimensions cross as a cartesian product — one row per combination:

```bash
curl -X POST localhost:8280/api/indicator-queries \
  -H "Content-Type: application/json" \
  -d '{
    "indicatorCode": "publications.count",
    "filters": [{ "dimension": "country", "operator": "EQ", "values": ["GR"] }],
    "groupBy": ["country", "accessStatus"],
    "timeSeries": { "grain": "YEAR", "from": "2023-01-01", "to": "2023-01-01" },
    "sort": [],
    "limit": null
  }'
```

## Discovering a dimension's allowed values

`GET /api/dimensions/{code}/members?search=&limit=` works the same way for every registered
dimension:

- `country` — real EU country list, sourced from `Stakeholder.country`.
- `initiativeType` — fixed 4-value list: `OA_PUBLICATION_POLICY`, `IMMEDIATE_OA_POLICY`,
  `FINANCIAL_STRATEGY`, `MONITORING_INITIATIVE`.
- `accessStatus` — fixed 4-value list: `OA`, `CLOSED`, `EMBARGOED`, `RESTRICTED` (not a binary
  OA/Closed split).

## Response shape (`IndicatorResult`)

Matches the "multiple countries" `oa.country_initiative_status` example above (`groupBy: ["country"]`,
2021–2023):

```json
{
  "metadata": { "indicatorCode": "oa.country_initiative_status", "label": "National OA Initiative Status", "valueType": "BOOLEAN", "unit": "NONE", "aggregation": "NONE" },
  "dimensions": ["country", "period"],
  "data": [
    { "dimensions": { "country": "GR", "period": "2021" }, "value": true },
    { "dimensions": { "country": "GR", "period": "2022" }, "value": true },
    { "dimensions": { "country": "GR", "period": "2023" }, "value": false }
  ],
  "execution": { "generatedAt": "2026-07-18T...", "partial": false }
}
```

`data[].dimensions` only contains the keys listed in `dimensions` — a key is present only if that
dimension was in `groupBy` (e.g. no `country` key when not grouping by country), and `period` is
present only when `timeSeries` was set (omitting it returns a single snapshot row instead).

Filter/group interaction, generalized to any dimension: `EQ` + one value, no matching `groupBy`
entry = one row aggregated over that single value. `IN` + multiple values, no matching `groupBy`
entry = one row aggregated over that whole subset. Adding the dimension to `groupBy` = one row per
value instead of one aggregated row — per the filtered subset if a filter was given, else every
member of that dimension's full domain.
