package eu.eosc.observatory.service;

import gr.athenarc.authorization.domain.AuthTriple;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public interface PermissionService {

    Set<String> getPermissions(String userId, String resourceId);

    Set<AuthTriple> addPermissions(List<String> users, List<String> actions, List<String> resourceIds);

    void removePermissions(List<String> users, List<String> actions, List<String> resourceIds);

    Set<AuthTriple> addManagers(List<String> users, List<String> resourceIds);

    Set<AuthTriple> addContributors(List<String> users, List<String> resourceIds);

    void removeAll(String user);

    void removeAll(List<String> users);

    void remove(String user, String action, String resourceId);

    boolean hasPermission(String user, String action, String resourceId);

    boolean canRead(String userId, String resourceId);

    boolean canWrite(String userId, String resourceId);

    boolean canManage(String userId, String resourceId);

    boolean canPublish(String userId, String resourceId);

    // FIXME: create new file
    enum Permissions {
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
}
