package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.openaire.OpenAireBooleanFlag;
import org.springframework.stereotype.Service;

/**
 * Mirrors {@link OpenAireBooleanFlag}'s YES/NO constants for the "goldOA" dimension used by
 * publications.count.
 */
@Service
public class GoldOaDimensionMemberProvider extends EnumDimensionMemberProvider<OpenAireBooleanFlag> {

    public GoldOaDimensionMemberProvider() {
        super(IndicatorCodes.GOLD_OA_DIMENSION, OpenAireBooleanFlag.class,
            flag -> flag == OpenAireBooleanFlag.YES ? "Yes" : "No");
    }
}
