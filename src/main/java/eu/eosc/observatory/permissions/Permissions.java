package eu.eosc.observatory.permissions;

import java.util.Arrays;

public enum Permissions {
    READ("read"),
    WRITE("write"),
    MANAGE("manage"),
    PUBLISH("publish");

    private final String typeValue;

    Permissions(final String type) {
        this.typeValue = type;
    }

    public String getKey() {
        return typeValue;
    }

    /**
     * @return the Enum representation for the given string.
     * @throws IllegalArgumentException if unknown string.
     */
    public static Permissions fromString(String s) throws IllegalArgumentException {
        return Arrays.stream(Permissions.values())
                .filter(v -> v.typeValue.equalsIgnoreCase(s))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
    }

    /**
     * Checks if the given {@link String} exists in the values of the enum.
     *
     * @return boolean
     */
    public static boolean exists(String s) {
        return Arrays.stream(Permissions.values())
                .anyMatch(v -> v.typeValue.equalsIgnoreCase(s));
    }
}
