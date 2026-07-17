package eu.openaire.observatory.indicator.handler;

import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;

/**
 * Port for indicators computed by an external service (e.g. an existing stats-computation tool).
 * Translates a validated plan into whatever the remote service's own query shape is, and adapts
 * its response back into IndicatorResult. The declared allowedAggregations/dimensions/grains on
 * the IndicatorDefinition for indicators behind this port must be kept in sync by hand with what
 * the remote service actually supports — there is no way to validate that automatically against a
 * black-box remote service.
 */
public interface RemoteIndicatorClient {
    IndicatorResult query(IndicatorExecutionPlan plan);
}
