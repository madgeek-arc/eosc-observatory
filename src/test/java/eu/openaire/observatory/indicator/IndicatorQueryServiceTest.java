package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.dimension.DimensionMemberProvider;
import eu.openaire.observatory.indicator.dimension.DimensionMemberProviderRegistry;
import eu.openaire.observatory.indicator.handler.IndicatorQueryHandler;
import eu.openaire.observatory.indicator.handler.IndicatorQueryHandlerRegistry;
import eu.openaire.observatory.indicator.handler.SupportsComparisonQueries;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.query.ComparisonMode;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.query.IndicatorQueryBuilder;
import eu.openaire.observatory.indicator.query.TimeSeriesRequest;
import eu.openaire.observatory.indicator.result.ComparisonValue;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.AllowAllIndicatorAuthorizationService;
import eu.openaire.observatory.indicator.validation.IndicatorQueryValidator;
import eu.openaire.observatory.indicator.validation.InvalidFilterShapeException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndicatorQueryServiceTest {
    private interface ComparableHandler extends IndicatorQueryHandler, SupportsComparisonQueries { }

    private final ComparableHandler handler = mock(ComparableHandler.class);
    private final DimensionMemberProvider provider = mock(DimensionMemberProvider.class);

    private IndicatorQueryService service() {
        when(handler.handlerKey()).thenReturn("count");
        when(provider.dimensionCode()).thenReturn("country");
        var handlers = new IndicatorQueryHandlerRegistry(List.of(handler));
        var providers = new DimensionMemberProviderRegistry(List.of(provider));
        var validator = new IndicatorQueryValidator(code -> IndicatorFixtures.countIndicator(code),
            code -> IndicatorFixtures.countryDimension(), new AllowAllIndicatorAuthorizationService(), handlers, providers);
        return new IndicatorQueryService(validator, handlers, new TimeSeriesNormalizer(providers),
            new TimeSeriesAggregationCalculator(), new ComparisonCalculator());
    }

    @Test
    void snapshotSummariesLeaveRowsAndExistingSummaryUnchanged() {
        var service = service();
        var existingSummary = List.of(new IndicatorDataPoint(Map.of("source", "provider"), 42L));
        for (List<IndicatorDataPoint> rows : List.of(
                List.<IndicatorDataPoint>of(),
                List.of(new IndicatorDataPoint(Map.of("country", "GR"), 10L),
                    new IndicatorDataPoint(Map.of("country", "GR"), 20L)))) {
            var raw = new IndicatorResult(null, List.of("country"), rows, null, existingSummary);
            when(handler.execute(any())).thenReturn(raw);
            var query = IndicatorQueryBuilder.forIndicator("count").withSeriesAggregations(AggregationType.SUM).build();
            assertThat(service.execute(query, level -> true)).isSameAs(raw);
        }
    }

    @Test
    void normalizesThenSummarizesThenComparesFullSeries() {
        var service = service();
        when(handler.execute(any())).thenReturn(new IndicatorResult(null, List.of("period"), List.of(
            new IndicatorDataPoint(Map.of("period", "2021"), 10L),
            new IndicatorDataPoint(Map.of("period", "2023"), 20L)), null));
        var query = IndicatorQueryBuilder.forIndicator("count")
            .withTimeSeries(new TimeSeriesRequest(2021, 2023, null))
            .withSeriesAggregations(AggregationType.AVG)
            .withComparisonMode(ComparisonMode.TWO_POINT).build();
        var result = service.execute(query, level -> true);
        assertThat(result.summary()).containsExactly(new IndicatorDataPoint(Map.of("aggregation", "AVG"), 10.0));
        assertThat(result.data()).containsExactly(new IndicatorDataPoint(
            Map.of("fromPeriod", "2021", "toPeriod", "2023"), new ComparisonValue(10L, 100.0)));
    }

    @Test
    void malformedLaterFilterPreventsDiscoveryAndExecution() {
        var service = service();
        var valid = new IndicatorFilter("country", FilterOperator.EQ, List.of("GR"));
        for (IndicatorFilter malformed : Arrays.asList(null,
                new IndicatorFilter("other", FilterOperator.IN, List.of()),
                new IndicatorFilter("country", FilterOperator.EQ, List.of("DE")))) {
            var query = new IndicatorQuery("count", null, Arrays.asList(valid, malformed),
                null, null, null, null, null, null);
            assertThatThrownBy(() -> service.execute(query, level -> true))
                .isInstanceOf(InvalidFilterShapeException.class);
        }
        verify(provider, never()).validCodes(any());
        verify(provider, never()).search(any(), any(), anyInt());
        verify(handler, never()).execute(any());
    }
}
