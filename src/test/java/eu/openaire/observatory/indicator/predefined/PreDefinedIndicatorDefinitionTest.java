package eu.openaire.observatory.indicator.predefined;

import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.ChartType;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.query.ComparisonMode;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.query.IndicatorQueryBuilder;
import eu.openaire.observatory.indicator.query.SortDirection;
import eu.openaire.observatory.indicator.query.SortRequest;
import eu.openaire.observatory.indicator.query.TimeSeriesRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreDefinedIndicatorDefinitionTest {

    private static PreDefinedIndicatorDefinition widget(Map<String, List<String>> presetFilters) {
        return new PreDefinedIndicatorDefinition(
            "oa-1", "OA Publication in Europe", WidgetGroup.OPEN_ACCESS_PUBLICATIONS,
            ChartType.SCALAR, true,
            "publications.access_status_share", presetFilters
        );
    }

    @Test
    void singleValuePresetFilterBecomesEq() {
        var definition = widget(Map.of("accessStatus", List.of("OA")));

        var query = definition.toIndicatorQuery(IndicatorQueryBuilder.forIndicator("ignored").build());

        assertThat(query.filters()).containsExactly(new IndicatorFilter("accessStatus", FilterOperator.EQ, List.of("OA")));
    }

    @Test
    void multiValuePresetFilterBecomesIn() {
        var definition = widget(Map.of("initiativeType", List.of("OPEN_DATA_POLICY", "OA_PUBLICATION_POLICY")));

        var query = definition.toIndicatorQuery(IndicatorQueryBuilder.forIndicator("ignored").build());

        assertThat(query.filters()).hasSize(1);
        var filter = query.filters().get(0);
        assertThat(filter.dimension()).isEqualTo("initiativeType");
        assertThat(filter.operator()).isEqualTo(FilterOperator.IN);
        assertThat(filter.values()).containsExactlyInAnyOrder("OPEN_DATA_POLICY", "OA_PUBLICATION_POLICY");
    }

    @Test
    void presetFiltersPrependedToRequestFilters() {
        var definition = widget(Map.of("accessStatus", List.of("OA")));
        var requestFilter = new IndicatorFilter("surveyYear", FilterOperator.EQ, List.of("2023"));
        var request = IndicatorQueryBuilder.forIndicator("ignored").withFilters(requestFilter).build();

        var query = definition.toIndicatorQuery(request);

        assertThat(query.filters()).hasSize(2);
        assertThat(query.filters().get(0).dimension()).isEqualTo("accessStatus");
        assertThat(query.filters().get(1)).isEqualTo(requestFilter);
    }

    @Test
    void widgetsOwnIndicatorCodeAlwaysWinsOverTheRequests() {
        var definition = widget(Map.of());
        var request = IndicatorQueryBuilder.forIndicator("some.other.code").build();

        var query = definition.toIndicatorQuery(request);

        assertThat(query.indicatorCode()).isEqualTo("publications.access_status_share");
    }

    @Test
    void everyOtherFieldPassesThroughUnchanged() {
        var definition = widget(Map.of());
        var timeSeries = new TimeSeriesRequest(2022, 2023, null);
        var sort = new SortRequest("period", SortDirection.ASC);
        var request = IndicatorQueryBuilder.forIndicator("ignored")
            .withAggregation(AggregationType.AVG)
            .withGroupBy("country")
            .withTimeSeries(timeSeries)
            .withComparisonMode(ComparisonMode.TWO_POINT)
            .withSort(sort)
            .withLimit(50)
            .withSeriesAggregations(AggregationType.AVG, AggregationType.MEDIAN)
            .build();

        var query = definition.toIndicatorQuery(request);

        assertThat(query.aggregation()).isEqualTo(AggregationType.AVG);
        assertThat(query.groupBy()).containsExactly("country");
        assertThat(query.timeSeries()).isEqualTo(timeSeries);
        assertThat(query.comparisonMode()).isEqualTo(ComparisonMode.TWO_POINT);
        assertThat(query.sort()).containsExactly(sort);
        assertThat(query.limit()).isEqualTo(50);
        assertThat(query.seriesAggregations()).containsExactly(AggregationType.AVG, AggregationType.MEDIAN);
    }

    @Test
    void throwsPresetFilterConflictExceptionWhenRequestFiltersAPresetDimension() {
        var definition = widget(Map.of("accessStatus", List.of("OA")));
        var request = IndicatorQueryBuilder.forIndicator("ignored")
            .withFilters(new IndicatorFilter("accessStatus", FilterOperator.EQ, List.of("Closed")))
            .build();

        assertThatThrownBy(() -> definition.toIndicatorQuery(request))
            .isInstanceOf(PresetFilterConflictException.class)
            .hasMessageContaining("oa-1")
            .hasMessageContaining("accessStatus");
    }
}
