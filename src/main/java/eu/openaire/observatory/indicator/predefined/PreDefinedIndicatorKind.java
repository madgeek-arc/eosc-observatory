package eu.openaire.observatory.indicator.predefined;

/** Presentation kind of a pre-defined dashboard widget — orthogonal to the backing indicator's own RenderHint. */
public enum PreDefinedIndicatorKind {
    INDICATOR("indicator"),
    TREND("trend"),
    MAP("map"),
    POLICY("policy");

    private final String wireValue;

    PreDefinedIndicatorKind(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
