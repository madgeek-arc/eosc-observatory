package eu.openaire.observatory.indicator.predefined;

import eu.openaire.observatory.indicator.IndicatorCatalog;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static eu.openaire.observatory.indicator.IndicatorCodes.ACCESS_STATUS_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.COUNTRY_INITIATIVE_STATUS;
import static eu.openaire.observatory.indicator.IndicatorCodes.COUNTRY_POLICY_STATUS;
import static eu.openaire.observatory.indicator.IndicatorCodes.DATASETS_ACCESS_STATUS_SHARE;
import static eu.openaire.observatory.indicator.IndicatorCodes.EU_COUNTRY_COVERAGE;
import static eu.openaire.observatory.indicator.IndicatorCodes.FINANCIAL_INVESTMENT;
import static eu.openaire.observatory.indicator.IndicatorCodes.INITIATIVE_TYPE_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.INVESTMENT_AREA_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.LONG_TERM_PRESERVATION_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE;
import static eu.openaire.observatory.indicator.IndicatorCodes.SOFTWARE_OPEN_SOURCE_SETS_COUNT;
import static eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorFormat.CATEGORY;
import static eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorFormat.CURRENCY;
import static eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorFormat.NUMBER;
import static eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorFormat.PERCENTAGE;
import static eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorFormat.YES_NO;
import static eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorKind.INDICATOR;
import static eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorKind.MAP;
import static eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorKind.TREND;

/**
 * Fixed catalog of concrete dashboard widgets for an Explore-style page — one row per widget, each
 * backed by a real {@link IndicatorCatalog} indicator plus the preset filter value(s) that
 * disambiguate it (see {@link PreDefinedIndicatorDefinition}). This is a translation layer, not new
 * query logic: every mapping below reuses indicator+filter combinations already wired and
 * live-verified in {@code SurveyStatsToolIndicatorQueryHandler} (its
 * {@code QUESTION_CODE_BY_INITIATIVE_TYPE}/{@code QUESTION_CODE_BY_INVESTMENT_AREA} maps) — see
 * {@code docs/INDICATORS.md} and {@code docs/INDICATOR_INVENTORY_STATUS.md} for the underlying
 * per-question verification history.
 *
 * <p>Two widgets from the original 69-row inventory are deliberately left out:
 * "National Financial Strategy on Long-term Data Preservation" (Question39) and "Financial
 * investments in Long-term Data Preservation" (Question88) — the preservation sub-topic only has
 * policy/monitoring initiativeType members today
 * ({@code REPOSITORIES_PRESERVATION_POLICY}/{@code REPOSITORIES_PRESERVATION_MONITORING_INITIATIVE}),
 * not a financial-strategy/investment-area member. Add
 * {@code REPOSITORIES_PRESERVATION_FINANCIAL_STRATEGY} (initiativeType) and
 * {@code REPOSITORIES_PRESERVATION} (investmentArea), each live-verified against the survey
 * stats-tool for 2022/2023, before adding those two back.
 */
@Service
public class PreDefinedIndicatorCatalog {

    private static final Map<String, List<String>> NO_FILTERS = Map.of();

    private final Map<String, PreDefinedIndicatorDefinition> definitionsById;

    public PreDefinedIndicatorCatalog(IndicatorCatalog indicatorCatalog) {
        Map<String, PreDefinedIndicatorDefinition> byId = new LinkedHashMap<>();
        for (PreDefinedIndicatorDefinition definition : allDefinitions()) {
            if (indicatorCatalog.get(definition.indicatorCode()) == null) {
                throw new IllegalStateException(
                    "PreDefinedIndicatorDefinition '" + definition.id() + "' references unknown indicatorCode: "
                        + definition.indicatorCode());
            }
            byId.put(definition.id(), definition);
        }
        this.definitionsById = Map.copyOf(byId);
    }

    public List<PreDefinedIndicatorDefinition> getAll() {
        return List.copyOf(definitionsById.values());
    }

    public PreDefinedIndicatorDefinition get(String id) {
        return definitionsById.get(id);
    }

    private static List<PreDefinedIndicatorDefinition> allDefinitions() {
        return List.of(
            // ---- Open Access Publications --------------------------------------------------------
            widget("oa-1", "OA Publication in Europe (OA vs Closed)", "Open Access Publications",
                INDICATOR, PERCENTAGE, true, PUBLICATIONS_ACCESS_STATUS_SHARE, accessStatus("OA")),
            widget("oa-2", "Trend of Open Access Publications", "Open Access Publications",
                TREND, PERCENTAGE, true, PUBLICATIONS_ACCESS_STATUS_SHARE, accessStatus("OA")),
            widget("oa-3", "EU Countries with Policy on OA Publication", "Open Access Publications",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("OA_PUBLICATION_POLICY")),
            widget("oa-4", "National Policy on Open Access Publications", "Open Access Publications",
                MAP, CATEGORY, false, COUNTRY_POLICY_STATUS, initiativeType("OA_PUBLICATION_POLICY")),
            widget("oa-5", "EU Countries with National OA Publication Monitoring Initiatives", "Open Access Publications",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("MONITORING_INITIATIVE")),
            widget("oa-6", "National Monitoring on Open Access Publications", "Open Access Publications",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("MONITORING_INITIATIVE")),
            widget("oa-7", "EU Countries with Financial Strategy on OA Publication", "Open Access Publications",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("FINANCIAL_STRATEGY")),
            widget("oa-8", "National Financial Strategy on Open Access Publications", "Open Access Publications",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("FINANCIAL_STRATEGY")),
            widget("oa-9", "EU countries with Policy on Immediate OA Publications", "Open Access Publications",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("IMMEDIATE_OA_POLICY")),
            widget("oa-10", "Financial investments in OA Publication", "Open Access Publications",
                INDICATOR, CURRENCY, true, FINANCIAL_INVESTMENT, investmentArea("OA_PUBLICATION")),

            // ---- Open Data -------------------------------------------------------------------------
            widget("od-1", "Open Data in Europe (Open vs Closed)", "Open Data",
                INDICATOR, PERCENTAGE, true, DATASETS_ACCESS_STATUS_SHARE, accessStatus("OA")),
            widget("od-2", "Trend of Open Data", "Open Data",
                TREND, PERCENTAGE, true, DATASETS_ACCESS_STATUS_SHARE, accessStatus("OA")),
            widget("od-3", "EU Countries with Policy on Open Data", "Open Data",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("OPEN_DATA_POLICY")),
            widget("od-4", "National Policy on Open Data", "Open Data",
                MAP, CATEGORY, false, COUNTRY_POLICY_STATUS, initiativeType("OPEN_DATA_POLICY")),
            widget("od-5", "EU Countries with National Open Data Monitoring Initiatives", "Open Data",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("OPEN_DATA_MONITORING_INITIATIVE")),
            widget("od-6", "National Monitoring on Open Data", "Open Data",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("OPEN_DATA_MONITORING_INITIATIVE")),
            widget("od-7", "EU Countries with Financial Strategy on Open Data", "Open Data",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("OPEN_DATA_FINANCIAL_STRATEGY")),
            widget("od-8", "National Financial Strategy on Open Data", "Open Data",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("OPEN_DATA_FINANCIAL_STRATEGY")),
            widget("od-9", "Financial investments in Open Data", "Open Data",
                INDICATOR, CURRENCY, true, FINANCIAL_INVESTMENT, investmentArea("OPEN_DATA")),

            // ---- FAIR Data -------------------------------------------------------------------------
            widget("fair-1", "EU Countries with Policy on FAIR Data", "FAIR Data",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("FAIR_DATA_POLICY")),
            widget("fair-2", "National Policy on FAIR Data", "FAIR Data",
                MAP, CATEGORY, false, COUNTRY_POLICY_STATUS, initiativeType("FAIR_DATA_POLICY")),
            widget("fair-3", "EU Countries with National FAIR Data Monitoring Initiatives", "FAIR Data",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("FAIR_DATA_MONITORING_INITIATIVE")),
            widget("fair-4", "National Monitoring on FAIR Data", "FAIR Data",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("FAIR_DATA_MONITORING_INITIATIVE")),
            widget("fair-5", "EU Countries with Financial Strategy on FAIR Data", "FAIR Data",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("FAIR_DATA_FINANCIAL_STRATEGY")),
            widget("fair-6", "National Financial Strategy on FAIR Data", "FAIR Data",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("FAIR_DATA_FINANCIAL_STRATEGY")),
            widget("fair-7", "Financial investments in FAIR Data", "FAIR Data",
                INDICATOR, CURRENCY, true, FINANCIAL_INVESTMENT, investmentArea("FAIR_DATA")),

            // ---- Data Management --------------------------------------------------------------------
            widget("dm-1", "EU Countries with Policy on Data Management", "Data Management",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("DATA_MANAGEMENT_POLICY")),
            widget("dm-2", "National Policy on Data Management", "Data Management",
                MAP, CATEGORY, false, COUNTRY_POLICY_STATUS, initiativeType("DATA_MANAGEMENT_POLICY")),
            widget("dm-3", "EU Countries with National Data Management Monitoring Initiatives", "Data Management",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("DATA_MANAGEMENT_MONITORING_INITIATIVE")),
            widget("dm-4", "National Monitoring on Data Management", "Data Management",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("DATA_MANAGEMENT_MONITORING_INITIATIVE")),
            widget("dm-5", "EU Countries with Financial Strategy on Data Management", "Data Management",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("DATA_MANAGEMENT_FINANCIAL_STRATEGY")),
            widget("dm-6", "National Financial Strategy on Data Management", "Data Management",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("DATA_MANAGEMENT_FINANCIAL_STRATEGY")),
            widget("dm-7", "Financial investments in Data Management", "Data Management",
                INDICATOR, CURRENCY, true, FINANCIAL_INVESTMENT, investmentArea("DATA_MANAGEMENT")),

            // ---- Citizen Science ---------------------------------------------------------------------
            widget("cs-1", "New Citizen Science projects in EU Countries", "Citizen Science",
                INDICATOR, NUMBER, true, CITIZEN_SCIENCE_NEW_PROJECTS_COUNT, NO_FILTERS),
            widget("cs-2", "EU Countries with Policy on Citizen Science", "Citizen Science",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("CITIZEN_SCIENCE_POLICY")),
            widget("cs-3", "National Policy on Citizen Science", "Citizen Science",
                MAP, CATEGORY, false, COUNTRY_POLICY_STATUS, initiativeType("CITIZEN_SCIENCE_POLICY")),
            widget("cs-4", "EU Countries with Citizen Science Monitoring Initiatives", "Citizen Science",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("CITIZEN_SCIENCE_MONITORING_INITIATIVE")),
            widget("cs-5", "National Monitoring on Citizen Science", "Citizen Science",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("CITIZEN_SCIENCE_MONITORING_INITIATIVE")),
            widget("cs-6", "EU Countries with Financial Strategy on Citizen Science", "Citizen Science",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("CITIZEN_SCIENCE_FINANCIAL_STRATEGY")),
            widget("cs-7", "National Financial Strategy on Citizen Science", "Citizen Science",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("CITIZEN_SCIENCE_FINANCIAL_STRATEGY")),
            widget("cs-8", "Financial investments in Citizen Science", "Citizen Science",
                INDICATOR, CURRENCY, true, FINANCIAL_INVESTMENT, investmentArea("CITIZEN_SCIENCE")),

            // ---- Repositories (covers both Connecting Repositories to EOSC and Long-term Data
            //      Preservation sub-topics) ------------------------------------------------------------
            widget("repo-1", "Repositories offering long-term preservation in EU Countries", "Repositories",
                INDICATOR, NUMBER, true, LONG_TERM_PRESERVATION_COUNT, NO_FILTERS),
            widget("repo-2", "EU Countries with Policy on Connecting Repositories to EOSC", "Repositories",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("REPOSITORIES_CONNECT_POLICY")),
            widget("repo-3", "National Policy on Connecting Repositories to EOSC", "Repositories",
                MAP, CATEGORY, false, COUNTRY_POLICY_STATUS, initiativeType("REPOSITORIES_CONNECT_POLICY")),
            widget("repo-4", "EU Countries with Monitoring Initiatives on Connecting Repositories to EOSC", "Repositories",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("REPOSITORIES_CONNECT_MONITORING_INITIATIVE")),
            widget("repo-5", "National Monitoring on Connecting Repositories to EOSC", "Repositories",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("REPOSITORIES_CONNECT_MONITORING_INITIATIVE")),
            widget("repo-6", "EU Countries with Financial Strategy on Connecting Repositories to EOSC", "Repositories",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("REPOSITORIES_CONNECT_FINANCIAL_STRATEGY")),
            widget("repo-7", "National Financial Strategy on Connecting Repositories to EOSC", "Repositories",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("REPOSITORIES_CONNECT_FINANCIAL_STRATEGY")),
            widget("repo-8", "Financial investments in connecting repositories to EOSC", "Repositories",
                INDICATOR, CURRENCY, true, FINANCIAL_INVESTMENT, investmentArea("REPOSITORIES_CONNECT")),
            widget("repo-9", "National Policy on Long-term Data Preservation", "Repositories",
                MAP, CATEGORY, false, COUNTRY_POLICY_STATUS, initiativeType("REPOSITORIES_PRESERVATION_POLICY")),
            widget("repo-10", "National Monitoring on Long-term Data Preservation", "Repositories",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("REPOSITORIES_PRESERVATION_MONITORING_INITIATIVE")),
            // repo-11 ("National Financial Strategy on Long-term Data Preservation", Question39) and
            // repo-12 ("Financial investments in Long-term Data Preservation", Question88) are omitted —
            // see the class javadoc: no REPOSITORIES_PRESERVATION_FINANCIAL_STRATEGY/REPOSITORIES_PRESERVATION
            // member exists yet.

            // ---- Open Science Training -----------------------------------------------------------
            widget("train-1", "EU Countries with Policy on skills/training for Open Science", "Open Science Training",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("TRAINING_POLICY")),
            widget("train-2", "National Policy on Skills/Training in Open Science", "Open Science Training",
                MAP, CATEGORY, false, COUNTRY_POLICY_STATUS, initiativeType("TRAINING_POLICY")),
            widget("train-3", "EU Countries with Monitoring Initiatives on skills/training in Open Science", "Open Science Training",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("TRAINING_MONITORING_INITIATIVE")),
            widget("train-4", "National Monitoring on Skills/Training in Open Science", "Open Science Training",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("TRAINING_MONITORING_INITIATIVE")),
            widget("train-5", "EU Countries with Financial Strategy on skills/training in Open Science", "Open Science Training",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("TRAINING_FINANCIAL_STRATEGY")),
            widget("train-6", "National Financial Strategy on Skills/Training in Open Science", "Open Science Training",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("TRAINING_FINANCIAL_STRATEGY")),
            widget("train-7", "Financial investments in skills/training for Open Science", "Open Science Training",
                INDICATOR, CURRENCY, true, FINANCIAL_INVESTMENT, investmentArea("TRAINING")),

            // ---- Open Software ----------------------------------------------------------------------
            // sw-1 ("Open Software sets published in EU Countries") was ambiguous in the legacy UI
            // between an OSO-stats query and survey Question73 (see explore-indicators.ts's own
            // comment) — resolved to the survey-backed software.open_source_sets_count, which was
            // built specifically for this self-reported per-country figure, distinct from
            // software.count's OpenAIRE-indexed record count (see docs/INDICATOR_INVENTORY_STATUS.md).
            widget("sw-1", "Open Software sets published in EU Countries", "Open Software",
                INDICATOR, NUMBER, false, SOFTWARE_OPEN_SOURCE_SETS_COUNT, NO_FILTERS),
            widget("sw-2", "EU Countries with Policy on Open Software", "Open Software",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("SOFTWARE_POLICY")),
            widget("sw-3", "National Policy on Open Source Software", "Open Software",
                MAP, CATEGORY, false, COUNTRY_POLICY_STATUS, initiativeType("SOFTWARE_POLICY")),
            widget("sw-4", "EU Countries with Monitoring Initiatives on Open Software", "Open Software",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("SOFTWARE_MONITORING_INITIATIVE")),
            widget("sw-5", "National Monitoring on Open Source Software", "Open Software",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("SOFTWARE_MONITORING_INITIATIVE")),
            widget("sw-6", "EU Countries with Financial Strategy on Open Software", "Open Software",
                INDICATOR, PERCENTAGE, false, EU_COUNTRY_COVERAGE, initiativeType("SOFTWARE_FINANCIAL_STRATEGY")),
            widget("sw-7", "National Financial Strategy on Open Software", "Open Software",
                MAP, YES_NO, false, COUNTRY_INITIATIVE_STATUS, initiativeType("SOFTWARE_FINANCIAL_STRATEGY")),
            widget("sw-8", "Financial investments in Open Software", "Open Software",
                INDICATOR, CURRENCY, true, FINANCIAL_INVESTMENT, investmentArea("SOFTWARE")),

            // ---- Financial Investment (Investments in EOSC page) -------------------------------------
            widget("fin-1", "Total amount of investments in EOSC and Open Science", "Financial Investment",
                INDICATOR, CURRENCY, false, FINANCIAL_INVESTMENT, NO_FILTERS)
        );
    }

    private static PreDefinedIndicatorDefinition widget(
            String id, String label, String group, PreDefinedIndicatorKind kind, PreDefinedIndicatorFormat format,
            boolean hasAverage, String indicatorCode, Map<String, List<String>> presetFilters) {
        return new PreDefinedIndicatorDefinition(id, label, group, kind, format, hasAverage, indicatorCode, presetFilters);
    }

    private static Map<String, List<String>> accessStatus(String value) {
        return Map.of(ACCESS_STATUS_DIMENSION, List.of(value));
    }

    private static Map<String, List<String>> initiativeType(String value) {
        return Map.of(INITIATIVE_TYPE_DIMENSION, List.of(value));
    }

    private static Map<String, List<String>> investmentArea(String value) {
        return Map.of(INVESTMENT_AREA_DIMENSION, List.of(value));
    }
}
