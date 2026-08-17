package eu.openaire.observatory.indicator.query;

import eu.openaire.observatory.indicator.model.AggregationType;

import java.util.List;

/**
 * Test-only fluent builder — avoids re-touching every call site's positional args each time
 * IndicatorQuery gains/loses a field (happened 3 times in one session). Not for production use;
 * the only production consumer deserializes IndicatorQuery from JSON, never constructs it.
 */
public final class IndicatorQueryBuilder {

    private final String indicatorCode;
    private AggregationType aggregation;
    private List<IndicatorFilter> filters = List.of();
    private List<String> groupBy = List.of();
    private TimeSeriesRequest timeSeries;
    private ComparisonMode comparisonMode;
    private List<SortRequest> sort = List.of();
    private Integer limit;

    private IndicatorQueryBuilder(String indicatorCode) {
        this.indicatorCode = indicatorCode;
    }

    public static IndicatorQueryBuilder forIndicator(String indicatorCode) {
        return new IndicatorQueryBuilder(indicatorCode);
    }

    public IndicatorQueryBuilder withAggregation(AggregationType aggregation) {
        this.aggregation = aggregation;
        return this;
    }

    public IndicatorQueryBuilder withFilters(IndicatorFilter... filters) {
        this.filters = List.of(filters);
        return this;
    }

    public IndicatorQueryBuilder withGroupBy(String... groupBy) {
        this.groupBy = List.of(groupBy);
        return this;
    }

    public IndicatorQueryBuilder withTimeSeries(TimeSeriesRequest timeSeries) {
        this.timeSeries = timeSeries;
        return this;
    }

    public IndicatorQueryBuilder withComparisonMode(ComparisonMode comparisonMode) {
        this.comparisonMode = comparisonMode;
        return this;
    }

    public IndicatorQueryBuilder withSort(SortRequest... sort) {
        this.sort = List.of(sort);
        return this;
    }

    public IndicatorQueryBuilder withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public IndicatorQuery build() {
        return new IndicatorQuery(indicatorCode, aggregation, filters, groupBy, timeSeries, comparisonMode, sort, limit);
    }
}
