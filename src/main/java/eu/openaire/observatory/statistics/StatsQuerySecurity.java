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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.configuration.security.MethodSecurityExpressions;
import eu.openaire.observatory.domain.User;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class StatsQuerySecurity {

    private static final Logger logger = LoggerFactory.getLogger(StatsQuerySecurity.class);
    private final MethodSecurityExpressions securityExpressions;
    private final GenericResourceService genericResourceService;
    private final StatsToolProperties statsToolProperties;
    private final ObjectMapper objectMapper;

    public StatsQuerySecurity(MethodSecurityExpressions securityExpressions,
                              GenericResourceService genericResourceService,
                              StatsToolProperties statsToolProperties) {
        this.securityExpressions = securityExpressions;
        this.genericResourceService = genericResourceService;
        this.statsToolProperties = statsToolProperties;
        this.objectMapper = new ObjectMapper();
    }

    public boolean authorize(String json, Authentication authentication) {
        boolean authorized = true;

        Optional<User> user = Optional.empty();
        try {
            user = Optional.of(User.of(authentication));
        } catch (Exception ignore) {
        }

        QueryRequest queryRequest = convert(json);

        // search if query matches
        for (QueryRequest.Series series : queryRequest.getSeries()) {
            if (!StringUtils.hasText(series.getQuery().getName())) {
                logger.warn("Given query is empty.");
            }
            for (StatsToolProperties.QueryAccess queryAccess : statsToolProperties.getQueryAccess()) {
                Pattern pattern = Pattern.compile(queryAccess.getQueryPattern());
                if (pattern.matcher(series.getQuery().getName()).matches()) {

                    switch (queryAccess.getAccess()) {
                        case OPEN -> authorized = true;
                        case CLOSED -> authorized = securityExpressions.isAdmin(authentication);
                        case RESTRICTED -> authorized = /*securityExpressions.isAdmin(authentication) ||*/
                                hasGroupAccess(user.orElse(null), queryAccess.getGroups());
                    }

                    if (authorized) {
                        break;
                    }
                }
            }
        }

        return authorized;
    }

    private QueryRequest convert(String json) {
        try {
            return objectMapper.readValue(json, QueryRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean hasGroupAccess(User user, List<StatsToolProperties.Group> groups) {
        if (user == null || groups == null || groups.isEmpty()) {
            return false;
        }
        for (StatsToolProperties.Group group : groups) {

            // check access using pattern - not implemented yet
            if (StringUtils.hasText(group.getPattern())) {
                throw new UnsupportedOperationException("Not implemented yet");
            }
            // check access without pattern
            else {
                FacetFilter filter = new FacetFilter();
                filter.setResourceType(group.getName());
                filter.addFilter("type", group.getType());
                filter.addFilter(group.getRole(), user.getId());

                List<?> results = genericResourceService.getResults(filter).getResults();
                if (!results.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
