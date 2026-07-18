package eu.openaire.observatory.indicator.mock;

import eu.openaire.observatory.indicator.dimension.DimensionMember;
import eu.openaire.observatory.indicator.dimension.DimensionMemberPage;
import eu.openaire.observatory.indicator.dimension.DimensionMemberProvider;
import eu.openaire.observatory.indicator.dimension.DimensionMemberProviderRegistry;
import eu.openaire.observatory.indicator.model.AggregationPolicy;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.IndicatorAccessLevel;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorDimensionBinding;
import eu.openaire.observatory.indicator.model.IndicatorExecutionBinding;
import eu.openaire.observatory.indicator.model.IndicatorSemanticType;
import eu.openaire.observatory.indicator.model.IndicatorStatus;
import eu.openaire.observatory.indicator.model.IndicatorValueType;
import eu.openaire.observatory.indicator.model.MissingPeriodHandling;
import eu.openaire.observatory.indicator.model.NullHandling;
import eu.openaire.observatory.indicator.model.RatioPolicy;
import eu.openaire.observatory.indicator.model.RenderHint;
import eu.openaire.observatory.indicator.model.TemporalBehavior;
import eu.openaire.observatory.indicator.model.TimeGrain;
import eu.openaire.observatory.indicator.model.TimePolicy;
import eu.openaire.observatory.indicator.model.UnitType;
import eu.openaire.observatory.indicator.model.ZeroDenominatorHandling;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.indicator.validation.QuerySecurityScope;
import eu.openaire.observatory.indicator.validation.ResolvedDimension;
import eu.openaire.observatory.indicator.validation.ResolvedFilter;
import eu.openaire.observatory.indicator.validation.ResolvedTimeRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class MockIndicatorQueryHandlerTest {

    private static final List<String> ALL_COUNTRIES = List.of("DE", "FR", "GR");
    private static final List<String> ALL_ACCESS_STATUSES = List.of("OA", "CLOSED", "EMBARGOED", "RESTRICTED");

    private final DimensionMemberProvider countryProvider = fixedProvider("country", ALL_COUNTRIES);
    private final DimensionMemberProvider accessStatusProvider = fixedProvider("accessStatus", ALL_ACCESS_STATUSES);

    private final MockIndicatorQueryHandler handler =
        new MockIndicatorQueryHandler(new DimensionMemberProviderRegistry(List.of(countryProvider, accessStatusProvider)));

    private static DimensionMemberProvider fixedProvider(String dimensionCode, List<String> codes) {
        return new DimensionMemberProvider() {
            @Override
            public String dimensionCode() {
                return dimensionCode;
            }

            @Override
            public DimensionMemberPage search(String indicatorCode, String searchText, int limit) {
                return new DimensionMemberPage(codes.stream().map(c -> new DimensionMember(c, c)).toList());
            }
        };
    }

    @Test
    void singleCountrySingleYear() {
        var timeRange = yearRange(2023, 2023);
        var filters = List.of(new ResolvedFilter("country", FilterOperator.EQ, List.of("GR")));

        var result = handler.execute(plan(financialInvestmentDefinition(), filters, List.of(), timeRange));

        assertThat(result.data()).hasSize(1);
        assertThat(result.dimensions()).containsExactly("period");
        assertThat(result.data().get(0).dimensions()).doesNotContainKey("country");
    }

    @Test
    void singleCountryMultipleYears() {
        var timeRange = yearRange(2021, 2023);
        var filters = List.of(new ResolvedFilter("country", FilterOperator.EQ, List.of("GR")));

        var result = handler.execute(plan(financialInvestmentDefinition(), filters, List.of(), timeRange));

        assertThat(result.data()).hasSize(3);
        assertThat(result.data()).extracting(dp -> dp.dimensions().get("period"))
            .containsExactlyInAnyOrder("2021", "2022", "2023");
    }

    @Test
    void multipleCountriesSingleYear() {
        var timeRange = yearRange(2023, 2023);
        var groupBy = List.of(new ResolvedDimension("country"));

        var result = handler.execute(plan(financialInvestmentDefinition(), List.of(), groupBy, timeRange));

        assertThat(result.data()).hasSize(ALL_COUNTRIES.size());
        assertThat(result.dimensions()).containsExactly("country", "period");
    }

    @Test
    void multipleCountriesMultipleYears() {
        var timeRange = yearRange(2021, 2023);
        var groupBy = List.of(new ResolvedDimension("country"));

        var result = handler.execute(plan(financialInvestmentDefinition(), List.of(), groupBy, timeRange));

        assertThat(result.data()).hasSize(ALL_COUNTRIES.size() * 3);
        var year2023ValuesByCountry = result.data().stream()
            .filter(dp -> "2023".equals(dp.dimensions().get("period")))
            .collect(Collectors.toMap(dp -> dp.dimensions().get("country"), IndicatorDataPoint::value));
        assertThat(year2023ValuesByCountry).hasSize(ALL_COUNTRIES.size());
        assertThat(Set.copyOf(year2023ValuesByCountry.values())).hasSize(ALL_COUNTRIES.size());
    }

    @Test
    void nonCountryBoundIndicatorAlwaysReturnsOneAggregateRowPerPeriod() {
        var timeRange = yearRange(2021, 2023);

        var result = handler.execute(plan(publicationsOaShareDefinition(), List.of(), List.of(), timeRange));

        assertThat(result.data()).hasSize(3);
        assertThat(result.dimensions()).containsExactly("period");
        assertThat(result.data()).noneMatch(dp -> dp.dimensions().containsKey("country"));
    }

    @Test
    void noTimeRangeReturnsSingleSnapshotRow() {
        var result = handler.execute(plan(publicationsOaShareDefinition(), List.of(), List.of(), null));

        assertThat(result.data()).hasSize(1);
        assertThat(result.dimensions()).isEmpty();
    }

    @Test
    void groupsByNonCountryDimension() {
        var timeRange = yearRange(2023, 2023);
        var groupBy = List.of(new ResolvedDimension("accessStatus"));

        var result = handler.execute(plan(publicationsCountDefinition(), List.of(), groupBy, timeRange));

        assertThat(result.dimensions()).containsExactly("accessStatus", "period");
        assertThat(result.data()).extracting(dp -> dp.dimensions().get("accessStatus"))
            .containsExactlyInAnyOrderElementsOf(ALL_ACCESS_STATUSES);
    }

    @Test
    void groupsByMultipleDimensionsAsACartesianProduct() {
        var timeRange = yearRange(2023, 2023);
        var groupBy = List.of(new ResolvedDimension("country"), new ResolvedDimension("accessStatus"));

        var result = handler.execute(plan(publicationsCountDefinition(), List.of(), groupBy, timeRange));

        assertThat(result.dimensions()).containsExactly("country", "accessStatus", "period");
        assertThat(result.data()).hasSize(ALL_COUNTRIES.size() * ALL_ACCESS_STATUSES.size());
    }

    @Test
    void differentInitiativeTypeFiltersProduceDifferentValuesForSameCountryAndYear() {
        var timeRange = yearRange(2023, 2023);
        var policyFilters = List.of(new ResolvedFilter("initiativeType", FilterOperator.EQ, List.of("OA_PUBLICATION_POLICY")));
        var monitoringFilters = List.of(new ResolvedFilter("initiativeType", FilterOperator.EQ, List.of("MONITORING_INITIATIVE")));

        var policyResult = handler.execute(plan(euCountryCoverageDefinition(), policyFilters, List.of(), timeRange));
        var monitoringResult = handler.execute(plan(euCountryCoverageDefinition(), monitoringFilters, List.of(), timeRange));

        assertThat(policyResult.data().get(0).value()).isNotEqualTo(monitoringResult.data().get(0).value());
    }

    private static ResolvedTimeRange yearRange(int fromYear, int toYear) {
        return new ResolvedTimeRange(
            TimeGrain.YEAR, LocalDate.of(fromYear, 1, 1), LocalDate.of(toYear, 1, 1), MissingPeriodHandling.ZERO_FILL
        );
    }

    private static IndicatorExecutionPlan plan(IndicatorDefinition definition, List<ResolvedFilter> filters,
                                                List<ResolvedDimension> groupBy, ResolvedTimeRange timeRange) {
        return new IndicatorExecutionPlan(
            definition, null, filters, groupBy, timeRange, QuerySecurityScope.unrestricted(), 1000
        );
    }

    /** oa.financial_investment-shaped: country filterable/groupable. */
    private static IndicatorDefinition financialInvestmentDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), "oa.financial_investment", "Financial Investment in OA Publication", "desc", "Publications",
            IndicatorValueType.DECIMAL, IndicatorSemanticType.MEASURE, UnitType.CURRENCY, RenderHint.SCALAR,
            new AggregationPolicy(AggregationType.SUM, Set.of(AggregationType.SUM), NullHandling.EXCLUDE),
            null,
            Set.of(new IndicatorDimensionBinding(
                "country", Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                Set.of(FilterOperator.EQ, FilterOperator.IN), false
            )),
            timePolicy(),
            new IndicatorExecutionBinding("mock", "oa.financial_investment"),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    /** publications.count-shaped: filterable/groupable by both country and accessStatus. */
    private static IndicatorDefinition publicationsCountDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), "publications.count", "Publications", "desc", "Publications",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, RenderHint.SCALAR,
            new AggregationPolicy(AggregationType.SUM, Set.of(AggregationType.SUM), NullHandling.EXCLUDE),
            null,
            Set.of(
                new IndicatorDimensionBinding(
                    "country", Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ, FilterOperator.IN), false
                ),
                new IndicatorDimensionBinding(
                    "accessStatus", Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ, FilterOperator.IN), false
                )
            ),
            timePolicy(),
            new IndicatorExecutionBinding("mock", "publications.count"),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    /** publications.oa_share-shaped: intrinsically Europe-wide, no country dimension. */
    private static IndicatorDefinition publicationsOaShareDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), "publications.oa_share", "OA Publication Share (Europe)", "desc", "Publications",
            IndicatorValueType.DECIMAL, IndicatorSemanticType.RATIO, UnitType.PERCENT, RenderHint.SCALAR,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            new RatioPolicy(ZeroDenominatorHandling.NULL),
            Set.of(),
            timePolicy(),
            new IndicatorExecutionBinding("mock", "publications.oa_share"),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    /** oa.eu_country_coverage-shaped: requires an initiativeType filter, no country dimension. */
    private static IndicatorDefinition euCountryCoverageDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), "oa.eu_country_coverage", "EU Country Coverage of OA Initiative", "desc", "Publications",
            IndicatorValueType.DECIMAL, IndicatorSemanticType.RATIO, UnitType.PERCENT, RenderHint.SCALAR,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            new RatioPolicy(ZeroDenominatorHandling.NULL),
            Set.of(new IndicatorDimensionBinding(
                "initiativeType", Set.of(DimensionUsage.FILTER), Set.of(FilterOperator.EQ), true
            )),
            timePolicy(),
            new IndicatorExecutionBinding("mock", "oa.eu_country_coverage"),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    private static TimePolicy timePolicy() {
        return new TimePolicy(
            true, "year", Set.of(TimeGrain.YEAR), TimeGrain.YEAR, TemporalBehavior.PERIOD_VALUE, MissingPeriodHandling.ZERO_FILL
        );
    }
}
