/**
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.openaire.observatory.statistics;

import eu.openaire.observatory.domain.UserGroup;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties("stats-tool")
public class StatsToolProperties {

    private String endpoint;
    private List<QueryAccess> queryAccess = new ArrayList<>();

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
        private Access access = Access.OPEN;
        private List<Group> groups = new ArrayList<>();


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
        private String name;
        private String role;
        private UserGroup.GroupType type;
        private String pattern;

        public String getName() {
            return name;
        }

        public Group setName(String name) {
            this.name = name;
            return this;
        }

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

    @PostConstruct
    void validateProperties() throws Exception {
        StringBuilder errors = new StringBuilder();
        for (int i = 0; i < this.getQueryAccess().size(); i++) {

            if (!StringUtils.hasText(this.getQueryAccess().get(i).getQueryPattern())) {
                errors.append(String.format("%n- stats-tool.query-access[%s].query-pattern", i));
            }
            if (this.getQueryAccess().get(i).getAccess() == null) {
                errors.append(String.format("%n- stats-tool.query-access[%s].access", i));
            } else {

                switch (this.getQueryAccess().get(i).getAccess()) {
                    case RESTRICTED -> { // RESTRICTED -> more properties required
                        for (int j = 0; j < this.getQueryAccess().get(i).getGroups().size(); j++) {
                            if (!StringUtils.hasText(this.getQueryAccess().get(i).getGroups().get(j).getName())) {
                                errors.append(String.format("%n- stats-tool.query-access[%s].groups[%s].name", i, j));
                            }
                            if (this.getQueryAccess().get(i).getGroups().get(j).type == null) {
                                errors.append(String.format("%n- stats-tool.query-access[%s].groups[%s].type", i, j));
                            }
                            if (!StringUtils.hasText(this.getQueryAccess().get(i).getGroups().get(j).getRole())) {
                                errors.append(String.format("%n- stats-tool.query-access[%s].groups[%s].role", i, j));
                            }
                        }
                    }
                    case CLOSED, OPEN -> {} // no other properties required
                }

            }
        }
        errors.append('\n');
        if (StringUtils.hasText(errors)) {
            throw new Exception("Could not start service because there are missing properties.\nMissing properties: " + errors);
        }
    }
}
