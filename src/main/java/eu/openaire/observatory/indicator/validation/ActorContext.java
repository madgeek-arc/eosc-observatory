package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.model.IndicatorAccessLevel;

public interface ActorContext {
    boolean hasAccess(IndicatorAccessLevel level);
}
