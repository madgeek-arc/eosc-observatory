package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.openaire.OpenAireAccessMode;
import org.springframework.stereotype.Service;

/**
 * Mirrors {@link OpenAireAccessMode}'s 4 constants for the "accessStatus" dimension used by
 * publications.count and publications.access_status_share. "OA" is the open side; "CLOSED",
 * "EMBARGOED" and "RESTRICTED" are considered as non-open statuses.
 */
@Service
public class AccessStatusDimensionMemberProvider extends EnumDimensionMemberProvider<OpenAireAccessMode> {

    public AccessStatusDimensionMemberProvider() {
        super(IndicatorCodes.ACCESS_STATUS_DIMENSION, OpenAireAccessMode.class, mode -> switch (mode) {
            case OA -> "Open Access";
            case CLOSED -> "Closed Access";
            case EMBARGOED -> "Embargoed";
            case RESTRICTED -> "Restricted";
        });
    }
}
