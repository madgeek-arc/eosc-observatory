package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.openaire.OpenAireOaLicenseStatus;
import org.springframework.stereotype.Service;

/**
 * Mirrors {@link OpenAireOaLicenseStatus}'s 5 constants for the "oaLicenseStatus" dimension used
 * by publications.oa_license_status_count.
 */
@Service
public class OaLicenseStatusDimensionMemberProvider extends EnumDimensionMemberProvider<OpenAireOaLicenseStatus> {

    public OaLicenseStatusDimensionMemberProvider() {
        super(IndicatorCodes.OA_LICENSE_STATUS_DIMENSION, OpenAireOaLicenseStatus.class, status -> switch (status) {
            case OA_WITH_LICENSE -> "Open Access with licence";
            case OA_WITHOUT_LICENSE -> "Open Access without licence";
            case EMBARGOED -> "Embargoed";
            case RESTRICTED -> "Restricted";
            case CLOSED -> "Closed Access";
        });
    }
}
