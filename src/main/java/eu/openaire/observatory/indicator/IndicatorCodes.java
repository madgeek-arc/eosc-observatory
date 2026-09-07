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
    public static final String INVESTMENT_AREA_DIMENSION = "investmentArea";
    public static final String ACCESS_STATUS_DIMENSION = "accessStatus";
    public static final String SURVEY_ID_DIMENSION = "surveyId";
    public static final String SURVEY_YEAR_DIMENSION = "surveyYear";
    public static final String CLASSIFICATION_DIMENSION = "classification";
    public static final String FIELD_OF_SCIENCE_DIMENSION = "fieldOfScience";
    public static final String GOLD_OA_DIMENSION = "goldOA";
    public static final String GREEN_OA_DIMENSION = "greenOA";
    public static final String OA_LICENSE_STATUS_DIMENSION = "oaLicenseStatus";

    public static final String PUBLICATIONS_COUNT = "publications.count";
    public static final String DATASETS_COUNT = "datasets.count";
    public static final String SOFTWARE_COUNT = "software.count";
    public static final String PUBLICATIONS_ACCESS_STATUS_SHARE = "publications.access_status_share";
    public static final String DATASETS_ACCESS_STATUS_SHARE = "datasets.access_status_share";
    public static final String PUBLICATIONS_OA_LICENSE_STATUS_COUNT = "publications.oa_license_status_count";
    // Routed to SurveyStatsToolIndicatorQueryHandler. Cross-topic despite the "oa." prefix — see
    // IndicatorDefinitionCatalog.countryInitiativeStatus()/euCountryCoverage()/financialInvestment() javadoc.
    public static final String FINANCIAL_INVESTMENT = "oa.financial_investment";
    public static final String COUNTRY_INITIATIVE_STATUS = "oa.country_initiative_status";
    public static final String EU_COUNTRY_COVERAGE = "oa.eu_country_coverage";
    // 4-state (MANDATORY/NON_MANDATORY/NO_POLICY/AWAITING_DATA) counterpart to
    // COUNTRY_INITIATIVE_STATUS, valid only for "*_POLICY" initiativeType members — see
    // IndicatorDefinitionCatalog.countryPolicyStatus() javadoc.
    public static final String COUNTRY_POLICY_STATUS = "oa.country_policy_status";
    // Cross-topic composites summing Yes-answers across every "*_MONITORING_INITIATIVE"/"*_POLICY"
    // initiativeType member for one country — see IndicatorDefinitionCatalog.monitoredAreasCount()/policyAreasCount().
    public static final String MONITORED_AREAS_COUNT = "oa.monitored_areas_count";
    public static final String POLICY_AREAS_COUNT = "oa.policy_areas_count";
    // Inventory row "Coverage of Researchers by National Open Science Policies in Europe": share of
    // Europe's researchers in countries with a national OS policy. Registered so the catalog covers
    // every inventory row, but not yet queryable — its EUROSTAT per-country researcher-count
    // denominator has no confirmed ingest in this repo. LocalRegistryIndicatorQueryHandler answers
    // it with a 501 until that data lands. See docs/INDICATOR_TODO.md.
    public static final String RESEARCHER_POLICY_COVERAGE = "oa.researcher_policy_coverage";
    public static final String LONG_TERM_PRESERVATION_COUNT = "repositories.long_term_preservation_count";
    public static final String CITIZEN_SCIENCE_NEW_PROJECTS_COUNT = "citizen_science.new_projects_count";
    // Self-reported per-country count (survey Question73), distinct from software.count's OpenAIRE
    // record count — see IndicatorDefinitionCatalog.softwareOpenSourceSetsCount() javadoc.
    public static final String SOFTWARE_OPEN_SOURCE_SETS_COUNT = "software.open_source_sets_count";
    // Routed to LocalRegistryIndicatorQueryHandler — no external HTTP call.
    public static final String COUNTRIES_WITH_VALIDATED_ANSWERS = "survey.countries_with_validated_answers";
    public static final String STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT = "stakeholder.participating_countries_count";
}
