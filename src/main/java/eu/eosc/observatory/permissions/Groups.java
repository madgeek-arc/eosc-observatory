package eu.eosc.observatory.permissions;

import java.util.Arrays;

public enum Groups {
    ADMINISTRATOR("admin"),
    COORDINATOR("coordinator"),
    STAKEHOLDER_CONTRIBUTOR("stakeholder_contributor"),
    STAKEHOLDER_MANAGER("stakeholder_manager");

    private final String group;

    Groups(final String type) {
        this.group = type;
    }

    public String getKey() {
        return group;
    }

    /**
     * @return the Enum representation for the given string.
     * @throws IllegalArgumentException if unknown string.
     */
    public static eu.eosc.observatory.permissions.Groups fromString(String s) throws IllegalArgumentException {
        return Arrays.stream(eu.eosc.observatory.permissions.Groups.values())
                .filter(v -> v.group.equalsIgnoreCase(s))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
    }

    /**
     * Checks if the given {@link String} exists in the values of the enum.
     *
     * @return boolean
     */
    public static boolean exists(String s) {
        return Arrays.stream(eu.eosc.observatory.permissions.Groups.values())
                .anyMatch(v -> v.group.equalsIgnoreCase(s));
    }
}
