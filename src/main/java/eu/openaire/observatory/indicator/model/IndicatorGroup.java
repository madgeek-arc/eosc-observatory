package eu.openaire.observatory.indicator.model;

/** Topic-area grouping of a preset indicator. */
public enum IndicatorGroup {
    OPEN_ACCESS_PUBLICATIONS("Open Access Publications"),
    OPEN_DATA("Open Data"),
    FAIR_DATA("FAIR Data"),
    DATA_MANAGEMENT("Data Management"),
    CITIZEN_SCIENCE("Citizen Science"),
    REPOSITORIES("Repositories"),
    OPEN_SCIENCE_TRAINING("Open Science Training"),
    OPEN_SOFTWARE("Open Software"),
    FINANCIAL_INVESTMENT("Financial Investment");

    private final String wireValue;

    IndicatorGroup(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
