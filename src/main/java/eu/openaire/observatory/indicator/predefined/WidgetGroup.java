package eu.openaire.observatory.indicator.predefined;

/** Topic-area grouping of a pre-defined dashboard widget, used to section an Explore-style page. */
public enum WidgetGroup {
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

    WidgetGroup(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
