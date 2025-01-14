package eu.eosc.observatory.statistics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.eosc.observatory.configuration.security.MethodSecurityExpressions;
import eu.eosc.observatory.domain.User;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.athenarc.catalogue.service.GenericItemService;
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
    private final GenericItemService genericItemService;
    private final StatsToolProperties statsToolProperties;
    private final ObjectMapper objectMapper;

    public StatsQuerySecurity(MethodSecurityExpressions securityExpressions,
                              @Qualifier(value = "catalogueGenericItemService") GenericItemService genericItemService,
                              StatsToolProperties statsToolProperties) {
        this.securityExpressions = securityExpressions;
        this.genericItemService = genericItemService;
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

                List<?> results = genericItemService.getResults(filter).getResults();
                if (!results.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
