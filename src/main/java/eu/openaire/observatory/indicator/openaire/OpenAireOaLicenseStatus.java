package eu.openaire.observatory.indicator.openaire;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The "oaLicenseStatus" dimension's 5 members, backing publications.oa_license_status_count (the
 * europe_monitor-profiled indicator behind observatory-ui's distributionOfOAPublications). Reuses
 * {@link OpenAireAccessMode}'s literals rather than duplicating them — "Open Access" splits into two
 * license-flag-narrowed statuses here, "Embargo"/"Restricted"/"Closed Access" pass through unchanged
 * (confirmed live: result.access mode plus, for the with/without-license split only, an extra
 * result.indi_result_oa_with_license.oa_with_license / ...oa_without_license = "1" filter).
 */
public enum OpenAireOaLicenseStatus {

    OA_WITH_LICENSE(OpenAireAccessMode.OA, "result.indi_result_oa_with_license.oa_with_license"),
    OA_WITHOUT_LICENSE(OpenAireAccessMode.OA, "result.indi_result_oa_without_license.oa_without_license"),
    EMBARGOED(OpenAireAccessMode.EMBARGOED, null),
    RESTRICTED(OpenAireAccessMode.RESTRICTED, null),
    CLOSED(OpenAireAccessMode.CLOSED, null);

    private static final Map<String, OpenAireOaLicenseStatus> BY_CATALOG_CODE = Stream.of(values())
        .collect(Collectors.toMap(Enum::name, status -> status));

    private final OpenAireAccessMode accessMode;
    private final String licenseField;

    OpenAireOaLicenseStatus(OpenAireAccessMode accessMode, String licenseField) {
        this.accessMode = accessMode;
        this.licenseField = licenseField;
    }

    public OpenAireAccessMode accessMode() {
        return accessMode;
    }

    /** The result.indi_result_oa_*_license.* field to additionally filter on ("1"), or null when access mode alone identifies this status. */
    public String licenseField() {
        return licenseField;
    }

    public static OpenAireOaLicenseStatus fromCatalogCode(String catalogCode) {
        OpenAireOaLicenseStatus status = BY_CATALOG_CODE.get(catalogCode);
        if (status == null) {
            throw new IllegalArgumentException("Unknown oaLicenseStatus code: " + catalogCode);
        }
        return status;
    }
}
