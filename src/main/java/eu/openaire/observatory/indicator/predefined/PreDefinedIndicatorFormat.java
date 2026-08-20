package eu.openaire.observatory.indicator.predefined;

/**
 * Display format of a pre-defined dashboard widget's value — a richer, UI-facing vocabulary than
 * IndicatorCatalogItem's 3-value format (percentage/number/map), distinguishing currency, category
 * (multi-value per-country status, e.g. oa.country_policy_status), and yes-no (binary/3-state
 * per-country status, e.g. oa.country_initiative_status).
 */
public enum PreDefinedIndicatorFormat {
    PERCENTAGE("percentage"),
    NUMBER("number"),
    CURRENCY("currency"),
    CATEGORY("category"),
    YES_NO("yes-no");

    private final String wireValue;

    PreDefinedIndicatorFormat(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
