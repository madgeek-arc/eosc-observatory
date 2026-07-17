package eu.openaire.observatory.indicator.handler;

import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;

public interface IndicatorQueryHandler {

    String indicatorCode();

    IndicatorResult execute(IndicatorExecutionPlan plan);
}
