package eu.eosc.observatory.statistics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.eosc.observatory.domain.UserInfo;
import eu.eosc.observatory.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class StatsQuerySecurity {

    private static final Logger logger = LoggerFactory.getLogger(StatsQuerySecurity.class);
    private final UserService userService;
    private final StatsToolProperties statsToolProperties;
    private final ObjectMapper objectMapper;

    public StatsQuerySecurity(UserService userService,
                              StatsToolProperties statsToolProperties) {
        this.userService = userService;
        this.statsToolProperties = statsToolProperties;
        this.objectMapper = new ObjectMapper();
    }

    public boolean authorize(String json, Authentication authentication) {
        boolean authorized = true;

        Optional<UserInfo> userInfo = Optional.empty();
        try {
            userInfo = Optional.of(userService.getUserInfo(authentication));
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
                        case CLOSED -> authorized = userInfo.isPresent() && userInfo.get().isAdmin();
                        case RESTRICTED -> authorized = /*userInfo.isPresent() && userInfo.get().isAdmin() ||*/
                                hasGroupAccess(userInfo.orElse(null), queryAccess.getGroups());
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

    private boolean hasGroupAccess(UserInfo userInfo, List<StatsToolProperties.Group> groups) {
        if (userInfo == null || groups == null || groups.isEmpty()) {
            return false;
        }
        for (StatsToolProperties.Group group : groups) {

            // check access using pattern - not implemented yet
            if (StringUtils.hasText(group.getPattern())) {
                throw new UnsupportedOperationException("Not implemented yet");
            }
            // check access without pattern
            else {
                if (
                        (group.getRole().equalsIgnoreCase("stakeholder") &&
                                userInfo.getStakeholders().stream().anyMatch(s -> s.getType().equals(group.getType())))
                                || (group.getRole().equalsIgnoreCase("coordinator") &&
                                userInfo.getCoordinators().stream().anyMatch(s -> s.getType().equals(group.getType())))) {
                    return true;
                }
            }
        }
        return false;
    }
}
