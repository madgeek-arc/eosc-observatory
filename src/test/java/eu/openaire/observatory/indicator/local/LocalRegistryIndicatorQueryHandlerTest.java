package eu.openaire.observatory.indicator.local;

import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.indicator.IndicatorCodes;
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
import eu.openaire.observatory.indicator.model.RenderHint;
import eu.openaire.observatory.indicator.model.TimePolicy;
import eu.openaire.observatory.indicator.model.UnitType;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.indicator.validation.QuerySecurityScope;
import eu.openaire.observatory.indicator.validation.ResolvedFilter;
import eu.openaire.observatory.service.StakeholderService;
import eu.openaire.observatory.service.SurveyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** No Spring context, no HTTP — SurveyService/StakeholderService are plain mocked interfaces. */
@ExtendWith(MockitoExtension.class)
class LocalRegistryIndicatorQueryHandlerTest {

    @Mock
    private SurveyService surveyService;
    @Mock
    private StakeholderService stakeholderService;

    private LocalRegistryIndicatorQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LocalRegistryIndicatorQueryHandler(surveyService, stakeholderService);
    }

    @Test
    void countriesWithValidatedAnswersDedupesCountries() {
        when(surveyService.getCountriesWithValidatedAnswer("m-eosc-sb-2023"))
            .thenReturn(List.of("GR", "GR", "DE", "FR"));
        var filters = List.of(new ResolvedFilter(IndicatorCodes.SURVEY_ID_DIMENSION, FilterOperator.EQ, List.of("m-eosc-sb-2023")));

        var result = handler.execute(plan(countriesWithValidatedAnswersDefinition(), filters));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).value()).isEqualTo(3L);
    }

    @Test
    void countriesWithValidatedAnswersThrowsWhenSurveyIdFilterMissing() {
        assertThatThrownBy(() -> handler.execute(plan(countriesWithValidatedAnswersDefinition(), List.of())))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void participatingCountriesCountFiltersByEoscSbTypeAndDedupesCountries() {
        Stakeholder gr1 = new Stakeholder();
        gr1.setId("gr-stakeholder-1");
        gr1.setCountry("GR");
        Stakeholder gr2 = new Stakeholder();
        gr2.setId("gr-stakeholder-2");
        gr2.setCountry("GR");
        Stakeholder de = new Stakeholder();
        de.setId("de-stakeholder");
        de.setCountry("DE");
        when(stakeholderService.getWithFilter("type", "eosc-sb")).thenReturn(Set.of(gr1, gr2, de));

        var result = handler.execute(plan(stakeholderParticipatingCountriesCountDefinition(), List.of()));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).value()).isEqualTo(2L);
        verify(stakeholderService).getWithFilter("type", "eosc-sb");
    }

    @Test
    void participatingCountriesCountReturnsZeroWhenNoStakeholdersMatch() {
        when(stakeholderService.getWithFilter("type", "eosc-sb")).thenReturn(null);

        var result = handler.execute(plan(stakeholderParticipatingCountriesCountDefinition(), List.of()));

        assertThat(result.data().get(0).value()).isEqualTo(0L);
    }

    private static IndicatorExecutionPlan plan(IndicatorDefinition definition, List<ResolvedFilter> filters) {
        return new IndicatorExecutionPlan(
            definition, AggregationType.NONE, filters, List.of(), null, null, QuerySecurityScope.unrestricted(), 1000
        );
    }

    private static TimePolicy nonTimeSeriesPolicy() {
        return new TimePolicy(false, MissingPeriodHandling.OMIT);
    }

    private static IndicatorDefinition countriesWithValidatedAnswersDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), IndicatorCodes.COUNTRIES_WITH_VALIDATED_ANSWERS,
            "Countries with Validated Survey Answers", "desc", "Survey",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, RenderHint.SCALAR,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            null,
            Set.of(new IndicatorDimensionBinding(
                IndicatorCodes.SURVEY_ID_DIMENSION, Set.of(DimensionUsage.FILTER), Set.of(FilterOperator.EQ), true
            )),
            nonTimeSeriesPolicy(),
            new IndicatorExecutionBinding(LocalRegistryIndicatorQueryHandler.HANDLER_KEY, IndicatorCodes.COUNTRIES_WITH_VALIDATED_ANSWERS),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    private static IndicatorDefinition stakeholderParticipatingCountriesCountDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), IndicatorCodes.STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT,
            "Participating Countries", "desc", "Stakeholders",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, RenderHint.SCALAR,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            null,
            Set.of(),
            nonTimeSeriesPolicy(),
            new IndicatorExecutionBinding(
                LocalRegistryIndicatorQueryHandler.HANDLER_KEY, IndicatorCodes.STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT
            ),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }
}
