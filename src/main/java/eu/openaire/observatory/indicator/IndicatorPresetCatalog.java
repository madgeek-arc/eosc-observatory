package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.ChartType;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorGroup;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

/** Fixed catalog of preset indicators, each an {@link IndicatorPreset} backed by an {@link IndicatorDefinition} from {@link IndicatorDefinitionCatalog}. */
@Service
public class IndicatorPresetCatalog {

    private static final Map<String, List<String>> NO_FILTERS = Map.of();
    private static final List<AggregationType> AVG_MEDIAN = List.of(AggregationType.AVG, AggregationType.MEDIAN);
    private static final List<AggregationType> NO_SERIES_AGGREGATIONS = List.of();

    /** {@code getAll()} contract: preset indicators grouped by {@link IndicatorGroup#order()}, then by {@link IndicatorPreset#order()} within a group. */
    private static final Comparator<IndicatorPreset> DISPLAY_ORDER =
        Comparator.comparingInt((IndicatorPreset preset) -> preset.group().order())
            .thenComparingInt(IndicatorPreset::order);

    private final Map<String, IndicatorPreset> presetsById;
    private final List<IndicatorPreset> ordered;

    public IndicatorPresetCatalog(IndicatorDefinitionCatalog definitionCatalog) {
        Map<String, IndicatorPreset> byId = new LinkedHashMap<>();
        Set<String> slots = new HashSet<>();
        for (IndicatorPreset preset : allPresets()) {
            IndicatorDefinition definition = definitionCatalog.get(preset.indicatorCode());
            if (definition == null) {
                throw new IllegalStateException(
                    "Preset indicator '" + preset.id() + "' references unknown indicatorCode: "
                        + preset.indicatorCode());
            }
            if (!slots.add(preset.group().name() + '#' + preset.order())) {
                throw new IllegalStateException(
                    "Two preset indicators claim order " + preset.order() + " in group " + preset.group()
                        + " (near id '" + preset.id() + "')");
            }
            byId.put(preset.id(), preset.resolvedAgainst(definition));
        }
        this.presetsById = Map.copyOf(byId);
        this.ordered = byId.values().stream().sorted(DISPLAY_ORDER).toList();
    }

    /** All preset indicators, grouped by topic in a fixed order and curated order within each group. */
    public List<IndicatorPreset> getAll() {
        return ordered;
    }

    public IndicatorPreset get(String id) {
        return presetsById.get(id);
    }

    private static List<IndicatorPreset> allPresets() {
        return List.of(
            // ---- Open Access Publications --------------------------------------------------------
            preset("oa-1", "OA Publication in Europe (OA vs Closed)", OPEN_ACCESS_PUBLICATIONS,
                SCALAR, AVG_MEDIAN, PUBLICATIONS_ACCESS_STATUS_SHARE, accessStatus("OA"), 10),
            preset("oa-2", "Trend of Open Access Publications", OPEN_ACCESS_PUBLICATIONS,
                LINE, AVG_MEDIAN, PUBLICATIONS_ACCESS_STATUS_SHARE, accessStatus("OA"), 20),
            preset("oa-3", "EU Countries with Policy on OA Publication", OPEN_ACCESS_PUBLICATIONS,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("OA_PUBLICATION_POLICY"), 30),
            preset("oa-4", "National Policy on Open Access Publications", OPEN_ACCESS_PUBLICATIONS,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("OA_PUBLICATION_POLICY"), 40),
            preset("oa-5", "EU Countries with National OA Publication Monitoring Initiatives", OPEN_ACCESS_PUBLICATIONS,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("MONITORING_INITIATIVE"), 50),
            preset("oa-6", "National Monitoring on Open Access Publications", OPEN_ACCESS_PUBLICATIONS,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("MONITORING_INITIATIVE"), 60),
            preset("oa-7", "EU Countries with Financial Strategy on OA Publication", OPEN_ACCESS_PUBLICATIONS,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("FINANCIAL_STRATEGY"), 70),
            preset("oa-8-x", "National Financial Strategy on Open Access Publications", OPEN_ACCESS_PUBLICATIONS,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("FINANCIAL_STRATEGY"), 80),
            preset("oa-9", "EU countries with Policy on Immediate OA Publications", OPEN_ACCESS_PUBLICATIONS,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("IMMEDIATE_OA_POLICY"), 90),
            preset("oa-10", "Financial investments in OA Publication", OPEN_ACCESS_PUBLICATIONS,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("OA_PUBLICATION"), 100),

            // ---- Open Data -------------------------------------------------------------------------
            preset("od-1", "Open Data in Europe (Open vs Closed)", OPEN_DATA,
                SCALAR, AVG_MEDIAN, DATASETS_ACCESS_STATUS_SHARE, accessStatus("OA"), 10),
            preset("od-2", "Trend of Open Data", OPEN_DATA,
                LINE, AVG_MEDIAN, DATASETS_ACCESS_STATUS_SHARE, accessStatus("OA"), 20),
            preset("od-3", "EU Countries with Policy on Open Data", OPEN_DATA,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("OPEN_DATA_POLICY"), 30),
            preset("od-4", "National Policy on Open Data", OPEN_DATA,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("OPEN_DATA_POLICY"), 40),
            preset("od-5", "EU Countries with National Open Data Monitoring Initiatives", OPEN_DATA,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("OPEN_DATA_MONITORING_INITIATIVE"), 50),
            preset("od-6", "National Monitoring on Open Data", OPEN_DATA,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("OPEN_DATA_MONITORING_INITIATIVE"), 60),
            preset("od-7", "EU Countries with Financial Strategy on Open Data", OPEN_DATA,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("OPEN_DATA_FINANCIAL_STRATEGY"), 70),
            preset("od-8-x", "National Financial Strategy on Open Data", OPEN_DATA,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("OPEN_DATA_FINANCIAL_STRATEGY"), 80),
            preset("od-9", "Financial investments in Open Data", OPEN_DATA,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("OPEN_DATA"), 90),

            // ---- FAIR Data -------------------------------------------------------------------------
            preset("fair-1", "EU Countries with Policy on FAIR Data", FAIR_DATA,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("FAIR_DATA_POLICY"), 10),
            preset("fair-2", "National Policy on FAIR Data", FAIR_DATA,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("FAIR_DATA_POLICY"), 20),
            preset("fair-3", "EU Countries with National FAIR Data Monitoring Initiatives", FAIR_DATA,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("FAIR_DATA_MONITORING_INITIATIVE"), 30),
            preset("fair-4", "National Monitoring on FAIR Data", FAIR_DATA,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("FAIR_DATA_MONITORING_INITIATIVE"), 40),
            preset("fair-5", "EU Countries with Financial Strategy on FAIR Data", FAIR_DATA,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("FAIR_DATA_FINANCIAL_STRATEGY"), 50),
            preset("fair-6-x", "National Financial Strategy on FAIR Data", FAIR_DATA,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("FAIR_DATA_FINANCIAL_STRATEGY"), 60),
            preset("fair-7", "Financial investments in FAIR Data", FAIR_DATA,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("FAIR_DATA"), 70),

            // ---- Data Management --------------------------------------------------------------------
            preset("dm-1", "EU Countries with Policy on Data Management", DATA_MANAGEMENT,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("DATA_MANAGEMENT_POLICY"), 10),
            preset("dm-2", "National Policy on Data Management", DATA_MANAGEMENT,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("DATA_MANAGEMENT_POLICY"), 20),
            preset("dm-3", "EU Countries with National Data Management Monitoring Initiatives", DATA_MANAGEMENT,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("DATA_MANAGEMENT_MONITORING_INITIATIVE"), 30),
            preset("dm-4", "National Monitoring on Data Management", DATA_MANAGEMENT,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("DATA_MANAGEMENT_MONITORING_INITIATIVE"), 40),
            preset("dm-5", "EU Countries with Financial Strategy on Data Management", DATA_MANAGEMENT,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("DATA_MANAGEMENT_FINANCIAL_STRATEGY"), 50),
            preset("dm-6-x", "National Financial Strategy on Data Management", DATA_MANAGEMENT,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("DATA_MANAGEMENT_FINANCIAL_STRATEGY"), 60),
            preset("dm-7", "Financial investments in Data Management", DATA_MANAGEMENT,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("DATA_MANAGEMENT"), 70),

            // ---- Citizen Science ---------------------------------------------------------------------
            preset("cs-1", "New Citizen Science projects in EU Countries", CITIZEN_SCIENCE,
                SCALAR, AVG_MEDIAN, CITIZEN_SCIENCE_NEW_PROJECTS_COUNT, NO_FILTERS, 10),
            preset("cs-2", "EU Countries with Policy on Citizen Science", CITIZEN_SCIENCE,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("CITIZEN_SCIENCE_POLICY"), 20),
            preset("cs-3", "National Policy on Citizen Science", CITIZEN_SCIENCE,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("CITIZEN_SCIENCE_POLICY"), 30),
            preset("cs-4", "EU Countries with Citizen Science Monitoring Initiatives", CITIZEN_SCIENCE,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("CITIZEN_SCIENCE_MONITORING_INITIATIVE"), 40),
            preset("cs-5", "National Monitoring on Citizen Science", CITIZEN_SCIENCE,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("CITIZEN_SCIENCE_MONITORING_INITIATIVE"), 50),
            preset("cs-6", "EU Countries with Financial Strategy on Citizen Science", CITIZEN_SCIENCE,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("CITIZEN_SCIENCE_FINANCIAL_STRATEGY"), 60),
            preset("cs-7-x", "National Financial Strategy on Citizen Science", CITIZEN_SCIENCE,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("CITIZEN_SCIENCE_FINANCIAL_STRATEGY"), 70),
            preset("cs-8", "Financial investments in Citizen Science", CITIZEN_SCIENCE,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("CITIZEN_SCIENCE"), 80),

            // ---- Repositories (covers both Connecting Repositories to EOSC and Long-term Data
            //      Preservation sub-topics) ------------------------------------------------------------
            preset("repo-1", "Repositories offering long-term preservation in EU Countries", REPOSITORIES,
                SCALAR, AVG_MEDIAN, LONG_TERM_PRESERVATION_COUNT, NO_FILTERS, 10),
            preset("repo-2", "EU Countries with Policy on Connecting Repositories to EOSC", REPOSITORIES,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("REPOSITORIES_CONNECT_POLICY"), 20),
            preset("repo-3", "National Policy on Connecting Repositories to EOSC", REPOSITORIES,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("REPOSITORIES_CONNECT_POLICY"), 30),
            preset("repo-4", "EU Countries with Monitoring Initiatives on Connecting Repositories to EOSC", REPOSITORIES,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("REPOSITORIES_CONNECT_MONITORING_INITIATIVE"), 40),
            preset("repo-5", "National Monitoring on Connecting Repositories to EOSC", REPOSITORIES,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("REPOSITORIES_CONNECT_MONITORING_INITIATIVE"), 50),
            preset("repo-6", "EU Countries with Financial Strategy on Connecting Repositories to EOSC", REPOSITORIES,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("REPOSITORIES_CONNECT_FINANCIAL_STRATEGY"), 60),
            preset("repo-7-x", "National Financial Strategy on Connecting Repositories to EOSC", REPOSITORIES,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("REPOSITORIES_CONNECT_FINANCIAL_STRATEGY"), 70),
            preset("repo-8", "Financial investments in connecting repositories to EOSC", REPOSITORIES,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("REPOSITORIES_CONNECT"), 80),
            preset("repo-9", "National Policy on Long-term Data Preservation", REPOSITORIES,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("REPOSITORIES_PRESERVATION_POLICY"), 90),
            preset("repo-10", "National Monitoring on Long-term Data Preservation", REPOSITORIES,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("REPOSITORIES_PRESERVATION_MONITORING_INITIATIVE"), 100),
            // No REPOSITORIES_PRESERVATION_FINANCIAL_STRATEGY/REPOSITORIES_PRESERVATION member exists yet,
            // so the preservation financial-strategy/investment presets are omitted.

            // ---- Open Science Training -----------------------------------------------------------
            preset("train-1", "EU Countries with Policy on skills/training for Open Science", OPEN_SCIENCE_TRAINING,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("TRAINING_POLICY"), 10),
            preset("train-2", "National Policy on Skills/Training in Open Science", OPEN_SCIENCE_TRAINING,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("TRAINING_POLICY"), 20),
            preset("train-3", "EU Countries with Monitoring Initiatives on skills/training in Open Science", OPEN_SCIENCE_TRAINING,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("TRAINING_MONITORING_INITIATIVE"), 30),
            preset("train-4", "National Monitoring on Skills/Training in Open Science", OPEN_SCIENCE_TRAINING,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("TRAINING_MONITORING_INITIATIVE"), 40),
            preset("train-5", "EU Countries with Financial Strategy on skills/training in Open Science", OPEN_SCIENCE_TRAINING,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("TRAINING_FINANCIAL_STRATEGY"), 50),
            preset("train-6-x", "National Financial Strategy on Skills/Training in Open Science", OPEN_SCIENCE_TRAINING,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("TRAINING_FINANCIAL_STRATEGY"), 60),
            preset("train-7", "Financial investments in skills/training for Open Science", OPEN_SCIENCE_TRAINING,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("TRAINING"), 70),

            // ---- Open Software ----------------------------------------------------------------------
            preset("sw-1", "Open Software sets published in EU Countries", OPEN_SOFTWARE,
                SCALAR, AVG_MEDIAN, SOFTWARE_OPEN_SOURCE_SETS_COUNT, NO_FILTERS, 10),
            preset("sw-2", "EU Countries with Policy on Open Software", OPEN_SOFTWARE,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("SOFTWARE_POLICY"), 20),
            preset("sw-3", "National Policy on Open Source Software", OPEN_SOFTWARE,
                POLICY, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("SOFTWARE_POLICY"), 30),
            preset("sw-4", "EU Countries with Monitoring Initiatives on Open Software", OPEN_SOFTWARE,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("SOFTWARE_MONITORING_INITIATIVE"), 40),
            preset("sw-5", "National Monitoring on Open Source Software", OPEN_SOFTWARE,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("SOFTWARE_MONITORING_INITIATIVE"), 50),
            preset("sw-6", "EU Countries with Financial Strategy on Open Software", OPEN_SOFTWARE,
                SCALAR, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("SOFTWARE_FINANCIAL_STRATEGY"), 60),
            preset("sw-7-x", "National Financial Strategy on Open Software", OPEN_SOFTWARE,
                MAP, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("SOFTWARE_FINANCIAL_STRATEGY"), 70),
            preset("sw-8", "Financial investments in Open Software", OPEN_SOFTWARE,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("SOFTWARE"), 80),

            // ---- Financial Investment (Investments in EOSC page) -------------------------------------
            preset("fin-1", "Total amount of investments in EOSC and Open Science", IndicatorGroup.FINANCIAL_INVESTMENT,
                SCALAR, AVG_MEDIAN, FINANCIAL_INVESTMENT, NO_FILTERS, 10)
        );
    }

    private static IndicatorPreset preset(
            String id, String label, IndicatorGroup group, ChartType chartType,
            List<AggregationType> allowedSeriesAggregations, String indicatorCode,
            Map<String, List<String>> presetFilters, int order) {
        // format is null here — IndicatorPresetCatalog fills it from the linked definition.
        return new IndicatorPreset(id, label, group, chartType, null,
            allowedSeriesAggregations, indicatorCode, presetFilters, order);
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
