package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.handler.IndicatorQueryHandlerRegistry;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.ActorContext;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.indicator.validation.IndicatorQueryValidator;

public class IndicatorQueryService {

    private final IndicatorQueryValidator validator;
    private final IndicatorQueryHandlerRegistry handlers;
    private final TimeSeriesNormalizer normalizer;

    public IndicatorQueryService(
            IndicatorQueryValidator validator,
            IndicatorQueryHandlerRegistry handlers,
            TimeSeriesNormalizer normalizer) {
        this.validator = validator;
        this.handlers = handlers;
        this.normalizer = normalizer;
    }

    public IndicatorResult execute(IndicatorQuery query, ActorContext actor) {
        IndicatorExecutionPlan plan = validator.compile(query, actor);
        var handler = handlers.getRequired(plan.definition().executionBinding().handlerKey());
        IndicatorResult result = handler.execute(plan);
        return normalizer.normalize(result, plan.timeRange());
    }
}
