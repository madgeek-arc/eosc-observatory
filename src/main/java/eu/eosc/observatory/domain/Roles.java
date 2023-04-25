package eu.eosc.observatory.domain;

public final class Roles {

    public interface Role {
        default String getRoleName() {
            return ((Enum<?>) this).name().toLowerCase();
        }
    }

    public enum Administrative implements Role {
        ADMINISTRATOR,
        SYSTEM
    }

    public enum Stakeholder implements Role {
        MANAGER,
        CONTRIBUTOR
    }

    public enum Coordinator implements Role {
        COORDINATOR_MEMBER;
    }

}
