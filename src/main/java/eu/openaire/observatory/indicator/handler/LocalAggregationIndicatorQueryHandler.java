package eu.openaire.observatory.indicator.handler;

import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;

public class LocalAggregationIndicatorQueryHandler implements IndicatorQueryHandler {

    private final String indicatorCode;
    private final IndicatorAnalyticsRepository repository;

    public LocalAggregationIndicatorQueryHandler(String indicatorCode, IndicatorAnalyticsRepository repository) {
        this.indicatorCode = indicatorCode;
        this.repository = repository;
    }

    @Override
    public String indicatorCode() {
        return indicatorCode;
    }

    @Override
    public IndicatorResult execute(IndicatorExecutionPlan plan) {
        return repository.aggregate(plan);
    }
}
