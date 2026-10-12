package eu.openaire.observatory.indicator.openaire;

import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.handler.AbstractIndicatorQueryHandler;
import eu.openaire.observatory.indicator.handler.RatioPercentage;
import eu.openaire.observatory.indicator.handler.RemoteIndicatorClient;
import eu.openaire.observatory.indicator.handler.SupportsComparisonQueries;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.indicator.validation.ResolvedDimension;
import eu.openaire.observatory.indicator.validation.ResolvedFilter;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Real handler for OpenAIRE-sourced indicators, serving
 * publications.access_status_share and datasets.access_status_share (both RATIO, requiring an
 * accessStatus filter to pick which share is returned — a single EQ value returns that exact
 * status's share; NOT_EQ or a multi-value IN aggregates several statuses together, e.g. NOT_EQ OA
 * for the full non-open share; same query shape, just entity=publication vs entity=dataset),
 * publications.count, datasets.count, and software.count (all MEASURE, optionally filtered/grouped
 * by accessStatus — the three are otherwise identical, just entity=publication vs entity=dataset vs
 * entity=software). All five support country as an EQ/IN filter (one or more countries OR'd
 * together; absent means all-EU, matching the always-applied continent=Europe scope), as a groupBy
 * (fans out into one row per country in a single extra sub-query, via OpenAireQueryBuilder's native
 * groupBy queries — not a query-per-country loop), or both at once — the filter then narrows which
 * countries the breakdown fans out over. publications.count/datasets.count/software.count
 * are also filterable/groupable by classification (document/output type), and publications.count
 * additionally by fieldOfScience.
 *
 * <p>publications.access_status_share's denominator ("total") is explicitly scoped to the 4 known
 * access-mode values (OA/Closed/Embargoed/Restricted, OR'd together), matching observatory-ui's own
 * OAPublicationVSClosed exactly — confirmed live against a 2023 mismatch where an unrestricted count
 * (no access-mode filter at all) pulled in ~21% more results than the legacy UI's definition of
 * "total", producing a materially wrong share. datasets.access_status_share reuses the exact same
 * denominator scoping, just entity=dataset.
 */
@Service
public class OpenAireStatsToolIndicatorQueryHandler extends AbstractIndicatorQueryHandler
        implements RemoteIndicatorClient, SupportsComparisonQueries {

    public static final String HANDLER_KEY = "openaire-stats-tool";

    private static final String ACCESS_STATUS_DIMENSION = IndicatorCodes.ACCESS_STATUS_DIMENSION;
    private static final String COUNTRY_DIMENSION = IndicatorCodes.COUNTRY_DIMENSION;
    private static final String CLASSIFICATION_DIMENSION = IndicatorCodes.CLASSIFICATION_DIMENSION;
    private static final String FIELD_OF_SCIENCE_DIMENSION = IndicatorCodes.FIELD_OF_SCIENCE_DIMENSION;
    private static final String GOLD_OA_DIMENSION = IndicatorCodes.GOLD_OA_DIMENSION;
    private static final String GREEN_OA_DIMENSION = IndicatorCodes.GREEN_OA_DIMENSION;
    private static final String OA_LICENSE_STATUS_DIMENSION = IndicatorCodes.OA_LICENSE_STATUS_DIMENSION;

    /** At most one of these is an active groupBy at a time — the legacy UI never breaks down by more than one. */
    private static final List<String> BREAKDOWN_DIMENSIONS =
        List.of(COUNTRY_DIMENSION, CLASSIFICATION_DIMENSION, FIELD_OF_SCIENCE_DIMENSION);

    private final OpenAireQueryBuilder queryBuilder;
    private final OpenAireStatsToolClient client;

    public OpenAireStatsToolIndicatorQueryHandler(OpenAireQueryBuilder queryBuilder, OpenAireStatsToolClient client) {
        this.queryBuilder = queryBuilder;
        this.client = client;
    }

    @Override
    public String handlerKey() {
        return HANDLER_KEY;
    }

    @Override
    public IndicatorResult execute(IndicatorExecutionPlan plan) {
        return query(plan);
    }

    @Override
    public IndicatorResult query(IndicatorExecutionPlan plan) {
        String providerIndicatorKey = plan.definition().executionBinding().providerIndicatorKey();
        return switch (providerIndicatorKey) {
            case IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE -> queryAccessStatusShare(plan, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);
            case IndicatorCodes.DATASETS_ACCESS_STATUS_SHARE -> queryAccessStatusShare(plan, OpenAireQueryBuilder.RESULT_TYPE_DATASET);
            case IndicatorCodes.PUBLICATIONS_COUNT -> queryResultCount(plan, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);
            case IndicatorCodes.DATASETS_COUNT -> queryResultCount(plan, OpenAireQueryBuilder.RESULT_TYPE_DATASET);
            case IndicatorCodes.SOFTWARE_COUNT -> queryResultCount(plan, OpenAireQueryBuilder.RESULT_TYPE_SOFTWARE);
            case IndicatorCodes.PUBLICATIONS_OA_LICENSE_STATUS_COUNT -> queryOaLicenseStatusCount(plan);
            default -> throw new IllegalStateException(
                "OpenAireStatsToolIndicatorQueryHandler has no logic for providerIndicatorKey: " + providerIndicatorKey);
        };
    }

    private IndicatorResult queryAccessStatusShare(IndicatorExecutionPlan plan, String resultType) {
        List<OpenAireAccessMode> numeratorModes = resolveNumeratorModes(plan);
        List<String> groupByCodes = plan.groupBy().stream().map(ResolvedDimension::dimensionCode).toList();
        List<Period> periods = resolvePeriods(plan);

        if (groupByCodes.contains(COUNTRY_DIMENSION)) {
            return queryAccessStatusShareByCountry(plan, numeratorModes, periods, groupByCodes, resultType);
        }

        DimensionFilters filters = DimensionFilters.ofCountry(filterValues(plan, COUNTRY_DIMENSION));
        List<ObjectNode> subQueries = new ArrayList<>();
        for (Period period : periods) {
            subQueries.add(queryBuilder.resultCountSubQuery(period.year(), numeratorModes, filters, resultType));
            subQueries.add(queryBuilder.resultCountSubQuery(period.year(), List.of(OpenAireAccessMode.values()), filters, resultType));
        }
        List<List<List<String>>> raw = client.fetchRaw(queryBuilder.seriesRequest(subQueries));

        List<IndicatorDataPoint> data = new ArrayList<>();
        for (int i = 0; i < periods.size(); i++) {
            long numerator = parseCount(raw, i * 2);
            long denominator = parseCount(raw, i * 2 + 1);
            Object value = RatioPercentage.of(numerator, denominator);
            data.add(new IndicatorDataPoint(periods.get(i).dimensions(), value));
        }

        return buildResult(plan, dimensionKeysFor(List.of(), plan), data);
    }

    private IndicatorResult queryAccessStatusShareByCountry(
            IndicatorExecutionPlan plan, List<OpenAireAccessMode> numeratorModes, List<Period> periods,
            List<String> groupByCodes, String resultType) {
        DimensionFilters filters = DimensionFilters.ofCountry(filterValues(plan, COUNTRY_DIMENSION));
        List<ObjectNode> subQueries = new ArrayList<>();
        for (Period period : periods) {
            subQueries.add(queryBuilder.resultCountByGroupSubQuery(
                period.year(), numeratorModes, resultType, COUNTRY_DIMENSION, filters));
            subQueries.add(queryBuilder.resultCountByGroupSubQuery(
                period.year(), List.of(OpenAireAccessMode.values()), resultType, COUNTRY_DIMENSION, filters));
        }
        List<List<List<String>>> raw = client.fetchRaw(queryBuilder.seriesRequest(subQueries));

        List<IndicatorDataPoint> data = new ArrayList<>();
        for (int i = 0; i < periods.size(); i++) {
            Map<String, Long> numeratorByCountry = countsByBreakdownValue(raw.get(i * 2));
            Map<String, Long> denominatorByCountry = countsByBreakdownValue(raw.get(i * 2 + 1));
            for (Map.Entry<String, Long> entry : denominatorByCountry.entrySet()) {
                String country = entry.getKey();
                long numerator = numeratorByCountry.getOrDefault(country, 0L);
                long denominator = entry.getValue();
                Object value = RatioPercentage.of(numerator, denominator);
                Map<String, Object> dimensions = new LinkedHashMap<>();
                dimensions.put(COUNTRY_DIMENSION, country);
                dimensions.putAll(periods.get(i).dimensions());
                data.add(new IndicatorDataPoint(dimensions, value));
            }
        }

        return buildResult(plan, dimensionKeysFor(groupByCodes, plan), data);
    }

    /**
     * Shared by publications.count and datasets.count — same query shape, just entity=publication vs
     * entity=dataset. At most one of country/classification/fieldOfScience is treated as the active
     * "fan out into one row per value" breakdown (whichever appears first in BREAKDOWN_DIMENSIONS).
     * A filter on any of the 3 always narrows the results, regardless of whether that same dimension
     * is also the active breakdown — filtering and grouping the same dimension composes exactly like
     * any other filter (narrows which values the breakdown fans out over), same as
     * country-filtered fieldOfScience/classification groupBy already did. The accessStatus
     * cross-product (one sub-query per requested access mode) composes independently, same as before
     * this method also handled non-country breakdowns.
     */
    private IndicatorResult queryResultCount(IndicatorExecutionPlan plan, String resultType) {
        List<String> groupByCodes = plan.groupBy().stream().map(ResolvedDimension::dimensionCode).toList();
        List<OpenAireAccessMode> accessModes = resolveAccessModeGroupValues(plan, groupByCodes);
        String breakdownDimension = BREAKDOWN_DIMENSIONS.stream().filter(groupByCodes::contains).findFirst().orElse(null);
        List<String> countryCodes = filterValues(plan, COUNTRY_DIMENSION);
        List<String> classificationCodes = filterValues(plan, CLASSIFICATION_DIMENSION);
        List<String> fieldOfScienceCodes = filterValues(plan, FIELD_OF_SCIENCE_DIMENSION);
        List<String> goldOaLiterals = filterValues(plan, GOLD_OA_DIMENSION).stream()
            .map(code -> OpenAireBooleanFlag.fromCatalogCode(code).openAireLiteral()).toList();
        List<String> greenOaLiterals = filterValues(plan, GREEN_OA_DIMENSION).stream()
            .map(code -> OpenAireBooleanFlag.fromCatalogCode(code).openAireLiteral()).toList();
        DimensionFilters filters = new DimensionFilters(countryCodes, classificationCodes, fieldOfScienceCodes, goldOaLiterals, greenOaLiterals);
        List<Period> periods = resolvePeriods(plan);

        record Cell(OpenAireAccessMode accessMode, Period period) {
        }

        List<Cell> cells = new ArrayList<>();
        List<ObjectNode> subQueries = new ArrayList<>();
        for (Period period : periods) {
            for (OpenAireAccessMode accessMode : accessModes) {
                cells.add(new Cell(accessMode, period));
                List<OpenAireAccessMode> modes = accessMode == null ? List.of() : List.of(accessMode);
                subQueries.add(breakdownDimension != null
                    ? queryBuilder.resultCountByGroupSubQuery(period.year(), modes, resultType, breakdownDimension, filters)
                    : queryBuilder.resultCountSubQuery(period.year(), modes, filters, resultType));
            }
        }
        List<List<List<String>>> raw = client.fetchRaw(queryBuilder.seriesRequest(subQueries));

        List<IndicatorDataPoint> data = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            Map<String, Object> baseDimensions = new LinkedHashMap<>();
            if (groupByCodes.contains(ACCESS_STATUS_DIMENSION)) {
                baseDimensions.put(ACCESS_STATUS_DIMENSION, cell.accessMode().name());
            }
            baseDimensions.putAll(cell.period().dimensions());

            if (breakdownDimension != null) {
                for (Map.Entry<String, Long> entry : countsByBreakdownValue(raw.get(i)).entrySet()) {
                    Map<String, Object> dimensions = new LinkedHashMap<>(baseDimensions);
                    dimensions.put(breakdownDimension, entry.getKey());
                    data.add(new IndicatorDataPoint(dimensions, entry.getValue()));
                }
            } else {
                data.add(new IndicatorDataPoint(baseDimensions, parseCount(raw, i)));
            }
        }

        return buildResult(plan, dimensionKeysFor(groupByCodes, plan), data);
    }

    /**
     * publications.oa_license_status_count: one sub-query per period per requested oaLicenseStatus,
     * always grouped by classification (its only mandatory groupable binding) — europe_monitor-profiled,
     * confirmed live. {@code oaLicenseStatus} is optional: an EQ/IN filter picks the status(es);
     * grouping by it instead fans out all five (used by the "distribution of access types" preset);
     * with neither, a single unfiltered total per classification is returned. An optional country
     * filter narrows to one or more specific countries instead of all-Europe; an optional country
     * groupBy additionally fans out into one row per (classification, country) pair — confirmed live
     * as a genuine two-select-group-field query against the europe_monitor profile, returning
     * {@code [count, classification, country]} rows.
     */
    private IndicatorResult queryOaLicenseStatusCount(IndicatorExecutionPlan plan) {
        List<String> groupByCodes = plan.groupBy().stream().map(ResolvedDimension::dimensionCode).toList();
        List<OpenAireOaLicenseStatus> statuses = resolveOaLicenseStatusGroupValues(plan, groupByCodes);
        List<String> countryCodes = filterValues(plan, COUNTRY_DIMENSION);
        boolean byStatus = groupByCodes.contains(OA_LICENSE_STATUS_DIMENSION);
        boolean byCountry = groupByCodes.contains(COUNTRY_DIMENSION);
        List<Period> periods = resolvePeriods(plan);

        List<ObjectNode> subQueries = new ArrayList<>();
        for (Period period : periods) {
            for (OpenAireOaLicenseStatus status : statuses) {
                subQueries.add(queryBuilder.oaLicenseStatusSubQuery(period.year(), status, countryCodes, byCountry));
            }
        }
        List<List<List<String>>> raw = client.fetchRaw(queryBuilder.seriesRequest(subQueries));

        List<IndicatorDataPoint> data = new ArrayList<>();
        for (int p = 0; p < periods.size(); p++) {
            for (int s = 0; s < statuses.size(); s++) {
                OpenAireOaLicenseStatus status = statuses.get(s);
                for (List<String> row : raw.get(p * statuses.size() + s)) {
                    Map<String, Object> dimensions = new LinkedHashMap<>();
                    if (byStatus && status != null) {
                        dimensions.put(OA_LICENSE_STATUS_DIMENSION, status.name());
                    }
                    dimensions.put(CLASSIFICATION_DIMENSION, row.get(1));
                    if (byCountry) {
                        dimensions.put(COUNTRY_DIMENSION, row.get(2));
                    }
                    dimensions.putAll(periods.get(p).dimensions());
                    data.add(new IndicatorDataPoint(dimensions, parseCountCell(row.get(0))));
                }
            }
        }

        List<String> dimensionKeys = new ArrayList<>();
        if (byStatus) {
            dimensionKeys.add(OA_LICENSE_STATUS_DIMENSION);
        }
        dimensionKeys.add(CLASSIFICATION_DIMENSION);
        if (byCountry) {
            dimensionKeys.add(COUNTRY_DIMENSION);
        }
        return buildResult(plan, dimensionKeysFor(dimensionKeys, plan), data);
    }

    /**
     * A matching EQ/IN {@code oaLicenseStatus} filter narrows to those status(es); a groupBy on
     * {@code oaLicenseStatus} fans out one sub-query per status (all five); neither means a single
     * unfiltered total per classification (the {@code null} element — the sub-query then omits the
     * access-mode/licence leaves). Mirrors {@link #resolveAccessModeGroupValues}.
     */
    private List<OpenAireOaLicenseStatus> resolveOaLicenseStatusGroupValues(IndicatorExecutionPlan plan, List<String> groupByCodes) {
        List<Object> filterValues = plan.filters().stream()
            .filter(filter -> filter.dimensionCode().equals(OA_LICENSE_STATUS_DIMENSION))
            .flatMap(filter -> filter.values().stream())
            .toList();
        if (!filterValues.isEmpty()) {
            return filterValues.stream().map(value -> OpenAireOaLicenseStatus.fromCatalogCode(String.valueOf(value))).toList();
        }
        if (groupByCodes.contains(OA_LICENSE_STATUS_DIMENSION)) {
            return List.of(OpenAireOaLicenseStatus.values());
        }
        return Collections.singletonList(null);
    }

    /** A matching EQ filter narrows to one value; a groupBy on accessStatus expands to all four; neither means unfiltered. */
    private List<OpenAireAccessMode> resolveAccessModeGroupValues(IndicatorExecutionPlan plan, List<String> groupByCodes) {
        List<Object> filterValues = plan.filters().stream()
            .filter(filter -> filter.dimensionCode().equals(ACCESS_STATUS_DIMENSION))
            .flatMap(filter -> filter.values().stream())
            .toList();
        if (!filterValues.isEmpty()) {
            return filterValues.stream().map(value -> OpenAireAccessMode.fromCatalogCode(String.valueOf(value))).toList();
        }
        if (groupByCodes.contains(ACCESS_STATUS_DIMENSION)) {
            return List.of(OpenAireAccessMode.values());
        }
        return Collections.singletonList(null);
    }

    /**
     * EQ/IN return exactly the requested status(es) OR'd together; NOT_EQ/NOT_IN return every status
     * except the requested one(s) — so a single EQ value is that exact status's share, while
     * NOT_EQ/NOT_IN "OA" (or an explicit IN over the three non-open literals) is the full non-open
     * share. NOT_EQ and NOT_IN are equivalent here: the domain is a small fixed enum, so "not this
     * one value" and "not any of these values" both reduce to the same in-memory complement.
     */
    private List<OpenAireAccessMode> resolveNumeratorModes(IndicatorExecutionPlan plan) {
        ResolvedFilter filter = requiredFilter(plan, ACCESS_STATUS_DIMENSION);
        List<OpenAireAccessMode> requested = filter.values().stream()
            .map(value -> OpenAireAccessMode.fromCatalogCode(String.valueOf(value)))
            .toList();
        return switch (filter.operator()) {
            case EQ, IN -> requested;
            case NOT_EQ, NOT_IN -> Arrays.stream(OpenAireAccessMode.values())
                .filter(mode -> !requested.contains(mode))
                .toList();
            default -> throw new IllegalStateException(
                "Unexpected accessStatus operator on publications.access_status_share: " + filter.operator());
        };
    }

    /** No timeRange -> one synthetic "current year" period with no period dimension; otherwise one period per year in range. */
    private List<Period> resolvePeriods(IndicatorExecutionPlan plan) {
        if (plan.timeRange() == null) {
            return List.of(new Period(Year.now().getValue(), Map.of()));
        }
        List<Period> periods = new ArrayList<>();
        for (int year = plan.timeRange().from(); year <= plan.timeRange().to(); year++) {
            periods.add(new Period(year, Map.of(PERIOD_DIMENSION, String.valueOf(year))));
        }
        return periods;
    }

    /** Parses [count, value] rows from a groupBy sub-query response into a value -> count map (value is a country code, classification, or field of science, depending on the breakdown dimension). */
    private Map<String, Long> countsByBreakdownValue(List<List<String>> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (List<String> row : rows) {
            counts.put(row.get(1), parseCountCell(row.get(0)));
        }
        return counts;
    }

    private long parseCount(List<List<List<String>>> raw, int seriesIndex) {
        List<List<String>> rows = raw.get(seriesIndex);
        if (rows.isEmpty() || rows.get(0).isEmpty()) {
            return 0L;
        }
        return parseCountCell(rows.get(0).get(0));
    }

    /** Parses one count cell; a missing, blank, or "null" cell means nothing was counted and yields 0. */
    private static long parseCountCell(String cell) {
        if (cell == null || cell.isBlank() || "null".equalsIgnoreCase(cell.trim())) {
            return 0L;
        }
        return Long.parseLong(cell.trim());
    }

    private record Period(int year, Map<String, Object> dimensions) {
    }
}
