# Shelved: gold/green/hybrid/bronze `oaColor` model

These files were an in-progress attempt to back the "Distribution of Open Access Types"
presets (`oa-9` / `od-8`) with an `oaColor` dimension (GOLD / GREEN / HYBRID / BRONZE) and new
`publications.oa_type_share` / `datasets.oa_type_share` RATIO indicators.

Shelved 2026-09 because:

- The legacy observatory-ui chart these presets render ("Distribution of Open Access Types by
  Different Scholarly Publication Outputs", `/explore/` → `distributionOfOAPublications`) is a
  **license / access-mode** breakdown — *Open Access with licence / without licence / Embargo /
  Restricted / Closed* stacked by document type — **not** an OA-colour breakdown.
- `HYBRID` / `BRONZE` map to stats-tool fields (`result.hybrid_oa` / `result.bronze_oa`) that
  do not exist under any name — a `groupBy:[oaColor]` always 422s.

`oa-9` / `od-8` are now backed by the license/access-status model instead
(`publications.oa_license_status_count` / `datasets.count`). Revisit these files only if the
stats-tool profile gains real `hybrid_oa` / `bronze_oa` fields and a genuine gold/green/hybrid/
bronze chart is wanted. The `.java.txt` extension keeps them out of the compiler.
