package eu.openaire.observatory.widget;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/** The dashboards that carry configurable widgets. Serialized as its kebab-case wire value. */
public enum DashboardCode {
    COUNTRY_PAGES("country-pages");

    private final String wireValue;

    DashboardCode(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    @JsonCreator
    public static DashboardCode fromWireValue(String value) {
        return Arrays.stream(values())
                .filter(c -> c.wireValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown dashboard code: " + value));
    }
}
