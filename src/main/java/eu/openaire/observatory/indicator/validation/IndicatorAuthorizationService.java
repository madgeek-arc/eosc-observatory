package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.query.IndicatorQuery;

/**
 * Derives the trusted, server-side row-level scope for a query. IndicatorQueryValidator calls this
 * during compile() and stamps the result onto the IndicatorExecutionPlan — the scope must never be
 * sourced from the request itself.
 */
public interface IndicatorAuthorizationService {
    QuerySecurityScope authorize(ActorContext actor, IndicatorDefinition definition, IndicatorQuery query);
}
