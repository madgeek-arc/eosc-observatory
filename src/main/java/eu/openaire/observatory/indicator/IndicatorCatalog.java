package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.ChartType;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorGroup;
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
import static eu.openaire.observatory.indicator.model.ChartType.LINE;
import static eu.openaire.observatory.indicator.model.ChartType.MAP;
import static eu.openaire.observatory.indicator.model.ChartType.POLICY;
import static eu.openaire.observatory.indicator.model.ChartType.SCALAR;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.CITIZEN_SCIENCE;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.DATA_MANAGEMENT;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.FAIR_DATA;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.OPEN_ACCESS_PUBLICATIONS;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.OPEN_DATA;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.OPEN_SCIENCE_TRAINING;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.OPEN_SOFTWARE;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.REPOSITORIES;

/** Fixed catalog of preset indicators, each an {@link Indicator} backed by an {@link IndicatorDefinition} from {@link IndicatorDefinitionCatalog}. */
@Service
public class IndicatorCatalog {

    private static final Map<String, List<String>> NO_FILTERS = Map.of();
    private static final List<AggregationType> AVG_MEDIAN = List.of(AggregationType.AVG, AggregationType.MEDIAN);
    private static final List<AggregationType> NO_SERIES_AGGREGATIONS = List.of();

    private final Map<String, Indicator> presetsById;

    public IndicatorCatalog(IndicatorDefinitionCatalog definitionCatalog) {
        Map<String, Indicator> byId = new LinkedHashMap<>();
        for (Indicator preset : allPresets()) {
            if (definitionCatalog.get(preset.indicatorCode()) == null) {
                throw new IllegalStateException(
                    "Indicator '" + preset.id() + "' references unknown indicatorCode: "
                        + preset.indicatorCode());
            }
            byId.put(preset.id(), preset);
        }
        this.presetsById = Map.copyOf(byId);
    }

    public List<Indicator> getAll() {
        return List.copyOf(presetsById.values());
    }

    public Indicator get(String id) {
        return presetsById.get(id);
    }

    private static List<Indicator> allPresets() {
        return List.of(
            // ---- Open Access Publications --------------------------------------------------------
            preset("oa-1", "OA Publication in Europe (OA vs Closed)", OPEN_ACCESS_PUBLICATIONS,
                SCALAR, AVG_MEDIAN, PUBLICATIONS_ACCESS_STATUS_SHARE, accessStatus("OA")),
            preset("oa-2", "Trend of Open Access Publications", OPEN_ACCESS_PUBLICATIONS,
                LINE, AVG_MEDIAN, PUBLICATIONS_ACCESS_STATUS_SHARE, accessStatus("OA")),
            preset("oa-3", "EU Countries with Policy on OA Publication", OPEN_ACCESS_PUBLICATIONS,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("OA_PUBLICATION_POLICY")),
            preset("oa-4", "National Policy on Open Access Publications", OPEN_ACCESS_PUBLICATIONS,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("OA_PUBLICATION_POLICY")),
            preset("oa-5", "EU Countries with National OA Publication Monitoring Initiatives", OPEN_ACCESS_PUBLICATIONS,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("MONITORING_INITIATIVE")),
            preset("oa-6", "National Monitoring on Open Access Publications", OPEN_ACCESS_PUBLICATIONS,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("MONITORING_INITIATIVE")),
            preset("oa-7", "EU Countries with Financial Strategy on OA Publication", OPEN_ACCESS_PUBLICATIONS,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("FINANCIAL_STRATEGY")),
            preset("oa-8-x", "National Financial Strategy on Open Access Publications", OPEN_ACCESS_PUBLICATIONS,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("FINANCIAL_STRATEGY")),
            preset("oa-9", "EU countries with Policy on Immediate OA Publications", OPEN_ACCESS_PUBLICATIONS,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("IMMEDIATE_OA_POLICY")),
            preset("oa-10", "Financial investments in OA Publication", OPEN_ACCESS_PUBLICATIONS,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("OA_PUBLICATION")),

            // ---- Open Data -------------------------------------------------------------------------
            preset("od-1", "Open Data in Europe (Open vs Closed)", OPEN_DATA,
                SCALAR, AVG_MEDIAN, DATASETS_ACCESS_STATUS_SHARE, accessStatus("OA")),
            preset("od-2", "Trend of Open Data", OPEN_DATA,
                LINE, AVG_MEDIAN, DATASETS_ACCESS_STATUS_SHARE, accessStatus("OA")),
            preset("od-3", "EU Countries with Policy on Open Data", OPEN_DATA,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("OPEN_DATA_POLICY")),
            preset("od-4", "National Policy on Open Data", OPEN_DATA,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("OPEN_DATA_POLICY")),
            preset("od-5", "EU Countries with National Open Data Monitoring Initiatives", OPEN_DATA,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("OPEN_DATA_MONITORING_INITIATIVE")),
            preset("od-6", "National Monitoring on Open Data", OPEN_DATA,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("OPEN_DATA_MONITORING_INITIATIVE")),
            preset("od-7", "EU Countries with Financial Strategy on Open Data", OPEN_DATA,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("OPEN_DATA_FINANCIAL_STRATEGY")),
            preset("od-8-x", "National Financial Strategy on Open Data", OPEN_DATA,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("OPEN_DATA_FINANCIAL_STRATEGY")),
            preset("od-9", "Financial investments in Open Data", OPEN_DATA,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("OPEN_DATA")),

            // ---- FAIR Data -------------------------------------------------------------------------
            preset("fair-1", "EU Countries with Policy on FAIR Data", FAIR_DATA,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("FAIR_DATA_POLICY")),
            preset("fair-2", "National Policy on FAIR Data", FAIR_DATA,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("FAIR_DATA_POLICY")),
            preset("fair-3", "EU Countries with National FAIR Data Monitoring Initiatives", FAIR_DATA,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("FAIR_DATA_MONITORING_INITIATIVE")),
            preset("fair-4", "National Monitoring on FAIR Data", FAIR_DATA,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("FAIR_DATA_MONITORING_INITIATIVE")),
            preset("fair-5", "EU Countries with Financial Strategy on FAIR Data", FAIR_DATA,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("FAIR_DATA_FINANCIAL_STRATEGY")),
            preset("fair-6-x", "National Financial Strategy on FAIR Data", FAIR_DATA,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("FAIR_DATA_FINANCIAL_STRATEGY")),
            preset("fair-7", "Financial investments in FAIR Data", FAIR_DATA,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("FAIR_DATA")),

            // ---- Data Management --------------------------------------------------------------------
            preset("dm-1", "EU Countries with Policy on Data Management", DATA_MANAGEMENT,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("DATA_MANAGEMENT_POLICY")),
            preset("dm-2", "National Policy on Data Management", DATA_MANAGEMENT,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("DATA_MANAGEMENT_POLICY")),
            preset("dm-3", "EU Countries with National Data Management Monitoring Initiatives", DATA_MANAGEMENT,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("DATA_MANAGEMENT_MONITORING_INITIATIVE")),
            preset("dm-4", "National Monitoring on Data Management", DATA_MANAGEMENT,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("DATA_MANAGEMENT_MONITORING_INITIATIVE")),
            preset("dm-5", "EU Countries with Financial Strategy on Data Management", DATA_MANAGEMENT,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("DATA_MANAGEMENT_FINANCIAL_STRATEGY")),
            preset("dm-6-x", "National Financial Strategy on Data Management", DATA_MANAGEMENT,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("DATA_MANAGEMENT_FINANCIAL_STRATEGY")),
            preset("dm-7", "Financial investments in Data Management", DATA_MANAGEMENT,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("DATA_MANAGEMENT")),

            // ---- Citizen Science ---------------------------------------------------------------------
            preset("cs-1", "New Citizen Science projects in EU Countries", CITIZEN_SCIENCE,
                SCALAR, AVG_MEDIAN, CITIZEN_SCIENCE_NEW_PROJECTS_COUNT, NO_FILTERS),
            preset("cs-2", "EU Countries with Policy on Citizen Science", CITIZEN_SCIENCE,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("CITIZEN_SCIENCE_POLICY")),
            preset("cs-3", "National Policy on Citizen Science", CITIZEN_SCIENCE,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("CITIZEN_SCIENCE_POLICY")),
            preset("cs-4", "EU Countries with Citizen Science Monitoring Initiatives", CITIZEN_SCIENCE,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("CITIZEN_SCIENCE_MONITORING_INITIATIVE")),
            preset("cs-5", "National Monitoring on Citizen Science", CITIZEN_SCIENCE,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("CITIZEN_SCIENCE_MONITORING_INITIATIVE")),
            preset("cs-6", "EU Countries with Financial Strategy on Citizen Science", CITIZEN_SCIENCE,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("CITIZEN_SCIENCE_FINANCIAL_STRATEGY")),
            preset("cs-7-x", "National Financial Strategy on Citizen Science", CITIZEN_SCIENCE,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("CITIZEN_SCIENCE_FINANCIAL_STRATEGY")),
            preset("cs-8", "Financial investments in Citizen Science", CITIZEN_SCIENCE,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("CITIZEN_SCIENCE")),

            // ---- Repositories (covers both Connecting Repositories to EOSC and Long-term Data
            //      Preservation sub-topics) ------------------------------------------------------------
            preset("repo-1", "Repositories offering long-term preservation in EU Countries", REPOSITORIES,
                SCALAR, AVG_MEDIAN, LONG_TERM_PRESERVATION_COUNT, NO_FILTERS),
            preset("repo-2", "EU Countries with Policy on Connecting Repositories to EOSC", REPOSITORIES,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("REPOSITORIES_CONNECT_POLICY")),
            preset("repo-3", "National Policy on Connecting Repositories to EOSC", REPOSITORIES,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("REPOSITORIES_CONNECT_POLICY")),
            preset("repo-4", "EU Countries with Monitoring Initiatives on Connecting Repositories to EOSC", REPOSITORIES,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("REPOSITORIES_CONNECT_MONITORING_INITIATIVE")),
            preset("repo-5", "National Monitoring on Connecting Repositories to EOSC", REPOSITORIES,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("REPOSITORIES_CONNECT_MONITORING_INITIATIVE")),
            preset("repo-6", "EU Countries with Financial Strategy on Connecting Repositories to EOSC", REPOSITORIES,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("REPOSITORIES_CONNECT_FINANCIAL_STRATEGY")),
            preset("repo-7-x", "National Financial Strategy on Connecting Repositories to EOSC", REPOSITORIES,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("REPOSITORIES_CONNECT_FINANCIAL_STRATEGY")),
            preset("repo-8", "Financial investments in connecting repositories to EOSC", REPOSITORIES,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("REPOSITORIES_CONNECT")),
            preset("repo-9", "National Policy on Long-term Data Preservation", REPOSITORIES,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("REPOSITORIES_PRESERVATION_POLICY")),
            preset("repo-10", "National Monitoring on Long-term Data Preservation", REPOSITORIES,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("REPOSITORIES_PRESERVATION_MONITORING_INITIATIVE")),
            // No REPOSITORIES_PRESERVATION_FINANCIAL_STRATEGY/REPOSITORIES_PRESERVATION member exists yet,
            // so the preservation financial-strategy/investment presets are omitted.

            // ---- Open Science Training -----------------------------------------------------------
            preset("train-1", "EU Countries with Policy on skills/training for Open Science", OPEN_SCIENCE_TRAINING,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("TRAINING_POLICY")),
            preset("train-2", "National Policy on Skills/Training in Open Science", OPEN_SCIENCE_TRAINING,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("TRAINING_POLICY")),
            preset("train-3", "EU Countries with Monitoring Initiatives on skills/training in Open Science", OPEN_SCIENCE_TRAINING,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("TRAINING_MONITORING_INITIATIVE")),
            preset("train-4", "National Monitoring on Skills/Training in Open Science", OPEN_SCIENCE_TRAINING,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("TRAINING_MONITORING_INITIATIVE")),
            preset("train-5", "EU Countries with Financial Strategy on skills/training in Open Science", OPEN_SCIENCE_TRAINING,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("TRAINING_FINANCIAL_STRATEGY")),
            preset("train-6-x", "National Financial Strategy on Skills/Training in Open Science", OPEN_SCIENCE_TRAINING,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("TRAINING_FINANCIAL_STRATEGY")),
            preset("train-7", "Financial investments in skills/training for Open Science", OPEN_SCIENCE_TRAINING,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("TRAINING")),

            // ---- Open Software ----------------------------------------------------------------------
            preset("sw-1", "Open Software sets published in EU Countries", OPEN_SOFTWARE,
                SCALAR, AVG_MEDIAN, SOFTWARE_OPEN_SOURCE_SETS_COUNT, NO_FILTERS),
            preset("sw-2", "EU Countries with Policy on Open Software", OPEN_SOFTWARE,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("SOFTWARE_POLICY")),
            preset("sw-3", "National Policy on Open Source Software", OPEN_SOFTWARE,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("SOFTWARE_POLICY")),
            preset("sw-4", "EU Countries with Monitoring Initiatives on Open Software", OPEN_SOFTWARE,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("SOFTWARE_MONITORING_INITIATIVE")),
            preset("sw-5", "National Monitoring on Open Source Software", OPEN_SOFTWARE,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("SOFTWARE_MONITORING_INITIATIVE")),
            preset("sw-6", "EU Countries with Financial Strategy on Open Software", OPEN_SOFTWARE,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("SOFTWARE_FINANCIAL_STRATEGY")),
            preset("sw-7-x", "National Financial Strategy on Open Software", OPEN_SOFTWARE,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("SOFTWARE_FINANCIAL_STRATEGY")),
            preset("sw-8", "Financial investments in Open Software", OPEN_SOFTWARE,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("SOFTWARE")),

            // ---- Financial Investment (Investments in EOSC page) -------------------------------------
            preset("fin-1", "Total amount of investments in EOSC and Open Science", IndicatorGroup.FINANCIAL_INVESTMENT,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, NO_FILTERS)
        );
    }

    private static Indicator preset(
            String id, String label, IndicatorGroup group, ChartType chartType,
            List<AggregationType> allowedSeriesAggregations, String indicatorCode, Map<String, List<String>> presetFilters) {
        return new Indicator(id, label, group, chartType, allowedSeriesAggregations, indicatorCode, presetFilters);
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
