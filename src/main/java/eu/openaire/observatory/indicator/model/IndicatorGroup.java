package eu.openaire.observatory.indicator.model;

import com.fasterxml.jackson.annotation.JsonValue;

/** Topic-area grouping of a preset indicator. {@code order} fixes the group sequence returned by {@code GET /indicators/presets}. */
public enum IndicatorGroup {
    OPEN_ACCESS_PUBLICATIONS("Open Access Publications", 10),
    OPEN_DATA("Open Data", 20),
    FAIR_DATA("FAIR Data", 30),
    DATA_MANAGEMENT("Data Management", 40),
    CITIZEN_SCIENCE("Citizen Science", 50),
    REPOSITORIES("Repositories", 60),
    OPEN_SCIENCE_TRAINING("Open Science Training", 70),
    OPEN_SOFTWARE("Open Software", 80),
    FINANCIAL_INVESTMENT("Financial Investment", 90);

    private final String wireValue;
    private final int order;

    IndicatorGroup(String wireValue, int order) {
        this.wireValue = wireValue;
        this.order = order;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    public int order() {
        return order;
    }
}
