package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.model.IndicatorAccessLevel;
import org.springframework.stereotype.Service;

/**
 * Placeholder actor: grants access to every indicator regardless of access level. Nothing derives a
 * real actor from the authenticated request yet — same known-limitation pattern as
 * AllowAllIndicatorAuthorizationService.
 */
@Service
public class AllowAllActorContext implements ActorContext {
    @Override
    public boolean hasAccess(IndicatorAccessLevel level) {
        return true;
    }
}
