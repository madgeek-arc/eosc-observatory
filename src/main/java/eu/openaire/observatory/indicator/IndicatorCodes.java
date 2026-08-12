package eu.openaire.observatory.indicator;

/**
 * Well-known dimension and indicator codes shared across the catalog and handlers. Deliberately not
 * owned by any one catalog/handler implementation so that a real handler doesn't need to depend on
 * catalog internals just to know these strings.
 */
public final class IndicatorCodes {

    private IndicatorCodes() {
    }

    public static final String COUNTRY_DIMENSION = "country";
    public static final String INITIATIVE_TYPE_DIMENSION = "initiativeType";
    public static final String ACCESS_STATUS_DIMENSION = "accessStatus";

    public static final String PUBLICATIONS_COUNT = "publications.count";
    public static final String PUBLICATIONS_ACCESS_STATUS_SHARE = "publications.access_status_share";
    // Not yet implemented — see docs/INDICATOR_ROADMAP.md. Kept as fixed identifiers so the eventual
    // real implementation reuses the same codes rather than retyping them from the roadmap doc.
    public static final String FINANCIAL_INVESTMENT = "oa.financial_investment";
    public static final String COUNTRY_INITIATIVE_STATUS = "oa.country_initiative_status";
    public static final String EU_COUNTRY_COVERAGE = "oa.eu_country_coverage";
}
