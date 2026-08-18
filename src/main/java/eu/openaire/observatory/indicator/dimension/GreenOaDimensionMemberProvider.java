package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.openaire.OpenAireBooleanFlag;
import org.springframework.stereotype.Service;

/**
 * Mirrors {@link OpenAireBooleanFlag}'s YES/NO constants for the "greenOA" dimension used by
 * publications.count.
 */
@Service
public class GreenOaDimensionMemberProvider extends EnumDimensionMemberProvider<OpenAireBooleanFlag> {

    public GreenOaDimensionMemberProvider() {
        super(IndicatorCodes.GREEN_OA_DIMENSION, OpenAireBooleanFlag.class,
            flag -> flag == OpenAireBooleanFlag.YES ? "Yes" : "No");
    }
}
