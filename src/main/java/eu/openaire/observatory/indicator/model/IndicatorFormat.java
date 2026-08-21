package eu.openaire.observatory.indicator.model;

/**
 * Display format of an indicator's value — a richer, per-indicator vocabulary distinguishing
 * currency, category (multi-value per-country status, e.g. oa.country_policy_status), and yes-no
 * (binary/3-state per-country status, e.g. oa.country_initiative_status), authored once on
 * IndicatorDefinition and shared by both the generic catalog and pre-defined dashboard widgets.
 */
public enum IndicatorFormat {
    PERCENTAGE("percentage"),
    NUMBER("number"),
    CURRENCY("currency"),
    CATEGORY("category"),
    YES_NO("yes-no");

    private final String wireValue;

    IndicatorFormat(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
