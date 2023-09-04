package eu.eosc.observatory.statistics;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties("stats-tool")
public class StatsToolProperties {

    private String endpoint;
    private String query;
    private Group[] groups;

    private Map<String, List<Group>> queryAccess;

    public Map<String, List<Group>> getQueryAccess() {
        return queryAccess;
    }

    public StatsToolProperties setQueryAccess(Map<String, List<Group>> queryAccess) {
        this.queryAccess = queryAccess;
        return this;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public StatsToolProperties setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public String getQuery() {
        return query;
    }

    public StatsToolProperties setQuery(String query) {
        this.query = query;
        return this;
    }

    public Group[] getGroups() {
        return groups;
    }

    public StatsToolProperties setGroups(Group[] groups) {
        this.groups = groups;
        return this;
    }

    public static class Group {
        private String role;
        private String type;
        private String access;
        private String pattern;

        public String getRole() {
            return role;
        }

        public Group setRole(String role) {
            this.role = role;
            return this;
        }

        public String getType() {
            return type;
        }

        public Group setType(String type) {
            this.type = type;
            return this;
        }

        public String getAccess() {
            return access;
        }

        public Group setAccess(String access) {
            this.access = access;
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
