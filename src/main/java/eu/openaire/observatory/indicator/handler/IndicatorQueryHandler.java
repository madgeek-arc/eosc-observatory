package eu.openaire.observatory.indicator.handler;

import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;

/**
 * One handler typically serves many indicators (e.g. one external service client for ~100
 * indicators) — routing is by handlerKey, not by indicator code. Which indicator within this
 * handler's source is being queried comes from {@code plan.definition().executionBinding()
 * .providerIndicatorKey()}. Implementations are expected to apply {@code plan.securityScope()} in
 * addition to {@code plan.filters()}.
 */
public interface IndicatorQueryHandler {

    String handlerKey();

    IndicatorResult execute(IndicatorExecutionPlan plan);
}
