package eu.eosc.observatory.statistics;

import eu.eosc.observatory.domain.UserGroup;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties("stats-tool")
public class StatsToolProperties {

    private String endpoint;
    private List<QueryAccess> queryAccess;

    public List<QueryAccess> getQueryAccess() {
        return queryAccess;
    }

    public void setQueryAccess(List<QueryAccess> queryAccess) {
        this.queryAccess = queryAccess;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public StatsToolProperties setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public enum Access {
        OPEN,
        CLOSED,
        RESTRICTED
    }

    public static class QueryAccess {
        private String queryPattern;
        private Access access;
        private List<Group> groups;


        public String getQueryPattern() {
            return queryPattern;
        }

        public void setQueryPattern(String queryPattern) {
            this.queryPattern = queryPattern;
        }

        public Access getAccess() {
            return access;
        }

        public void setAccess(Access access) {
            this.access = access;
        }

        public List<Group> getGroups() {
            return groups;
        }

        public void setGroups(List<Group> groups) {
            this.groups = groups;
        }
    }

    public static class Group {
        private String role;
        private UserGroup.GroupType type;
        private String pattern;

        public String getRole() {
            return role;
        }

        public Group setRole(String role) {
            this.role = role;
            return this;
        }

        public String getType() {
            return type.getKey();
        }

        public Group setType(UserGroup.GroupType type) {
            this.type = type;
            return this;
        }

        public String getPattern() {
            return pattern;
        }

        public Group setPattern(String pattern) {
            this.pattern = pattern;
            return this;
        }
    }
}
