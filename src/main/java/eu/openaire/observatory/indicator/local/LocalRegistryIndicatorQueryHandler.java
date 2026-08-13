package eu.openaire.observatory.indicator.local;

import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.handler.AbstractIndicatorQueryHandler;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.service.StakeholderService;
import eu.openaire.observatory.service.SurveyService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Real handler for the 2 "local registry" indicators (survey.countries_with_validated_answers,
 * stakeholder.participating_countries_count) — unlike every other handler in this package, neither
 * makes an external HTTP call: both wrap existing local services directly (SurveyService,
 * StakeholderService), which in turn talk to this app's own registry backend.
 *
 * <p>stakeholder.participating_countries_count is hardcoded to stakeholderType "eosc-sb"
 * (UserGroup.GroupType.EOSC_SB) — the EOSC Steering Board country representatives — rather than
 * exposed as a client-filterable dimension, so this handler calls
 * {@link StakeholderService#getWithFilter(String, String)} (inherited from CrudService) directly
 * instead of promoting a type-filtering method into StakeholderService.
 */
@Service
public class LocalRegistryIndicatorQueryHandler extends AbstractIndicatorQueryHandler {

    public static final String HANDLER_KEY = "local-registry";

    private static final String EOSC_SB_STAKEHOLDER_TYPE = "eosc-sb";

    private final SurveyService surveyService;
    private final StakeholderService stakeholderService;

    public LocalRegistryIndicatorQueryHandler(SurveyService surveyService, StakeholderService stakeholderService) {
        this.surveyService = surveyService;
        this.stakeholderService = stakeholderService;
    }

    @Override
    public String handlerKey() {
        return HANDLER_KEY;
    }

    @Override
    public IndicatorResult execute(IndicatorExecutionPlan plan) {
        String providerIndicatorKey = plan.definition().executionBinding().providerIndicatorKey();
        return switch (providerIndicatorKey) {
            case IndicatorCodes.COUNTRIES_WITH_VALIDATED_ANSWERS -> queryCountriesWithValidatedAnswers(plan);
            case IndicatorCodes.STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT -> queryParticipatingCountriesCount(plan);
            default -> throw new IllegalStateException(
                "LocalRegistryIndicatorQueryHandler has no logic for providerIndicatorKey: " + providerIndicatorKey);
        };
    }

    /** Deduplicated — getCountriesWithValidatedAnswer returns one entry per validated answer, not per distinct country. */
    private IndicatorResult queryCountriesWithValidatedAnswers(IndicatorExecutionPlan plan) {
        String surveyId = requiredFilterValue(plan, IndicatorCodes.SURVEY_ID_DIMENSION);
        long count = surveyService.getCountriesWithValidatedAnswer(surveyId).stream().distinct().count();
        return flatResult(plan, count);
    }

    private IndicatorResult queryParticipatingCountriesCount(IndicatorExecutionPlan plan) {
        Set<Stakeholder> stakeholders = stakeholderService.getWithFilter("type", EOSC_SB_STAKEHOLDER_TYPE);
        long count = stakeholders == null ? 0 : stakeholders.stream()
            .map(Stakeholder::getCountry)
            .filter(Objects::nonNull)
            .distinct()
            .count();
        return flatResult(plan, count);
    }

    private static IndicatorResult flatResult(IndicatorExecutionPlan plan, long value) {
        return buildResult(plan, List.of(), List.of(new IndicatorDataPoint(Map.of(), value)));
    }
}
