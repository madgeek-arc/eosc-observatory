package eu.openaire.observatory.indicator.handler;

import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;

/** Port for locally computed indicators — backed by SQL/JPA/jOOQ, decided later. */
public interface IndicatorAnalyticsRepository {
    IndicatorResult aggregate(IndicatorExecutionPlan plan);
}
