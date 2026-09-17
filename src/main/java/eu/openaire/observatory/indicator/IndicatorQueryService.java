package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.handler.IndicatorQueryHandlerRegistry;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.ActorContext;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.indicator.validation.IndicatorQueryValidator;
import org.springframework.stereotype.Service;

@Service
public class IndicatorQueryService {

    private final IndicatorQueryValidator validator;
    private final IndicatorQueryHandlerRegistry handlers;
    private final TimeSeriesNormalizer normalizer;
    private final TimeSeriesAggregationCalculator seriesAggregationCalculator;
    private final ComparisonCalculator comparisonCalculator;

    public IndicatorQueryService(
            IndicatorQueryValidator validator,
            IndicatorQueryHandlerRegistry handlers,
            TimeSeriesNormalizer normalizer,
            TimeSeriesAggregationCalculator seriesAggregationCalculator,
            ComparisonCalculator comparisonCalculator) {
        this.validator = validator;
        this.handlers = handlers;
        this.normalizer = normalizer;
        this.seriesAggregationCalculator = seriesAggregationCalculator;
        this.comparisonCalculator = comparisonCalculator;
    }

    public IndicatorResult execute(IndicatorQuery query, ActorContext actor) {
        IndicatorExecutionPlan plan = validator.compile(query, actor);
        var handler = handlers.getRequired(plan.definition().executionBinding().handlerKey());
        IndicatorResult result = handler.execute(plan);
        result = normalizer.normalize(result, plan.timeRange(), plan.filters());
        if (plan.timeRange() != null) {
            result = seriesAggregationCalculator.summarize(result, plan.seriesAggregations());
        }
        return comparisonCalculator.compare(result, plan.comparisonMode(), plan.timeRange(), plan.definition().semanticType());
    }
}
