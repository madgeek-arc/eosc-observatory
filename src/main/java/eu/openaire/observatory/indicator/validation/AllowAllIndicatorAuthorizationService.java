package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.query.IndicatorQuery;

/**
 * Placeholder authorization: grants an unrestricted scope to every actor/indicator. Real row-level
 * enforcement (tenant/org/country) is not implemented yet — only the seam exists so handlers and
 * the validator can depend on IndicatorAuthorizationService from day one.
 */
public class AllowAllIndicatorAuthorizationService implements IndicatorAuthorizationService {
    @Override
    public QuerySecurityScope authorize(ActorContext actor, IndicatorDefinition definition, IndicatorQuery query) {
        return QuerySecurityScope.unrestricted();
    }
}
