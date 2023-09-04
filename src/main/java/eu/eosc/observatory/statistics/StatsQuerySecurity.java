package eu.eosc.observatory.statistics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.eosc.observatory.domain.UserInfo;
import eu.eosc.observatory.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatsQuerySecurity {

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
        UserInfo userInfo = userService.getUserInfo(authentication);
        QueryRequest queryRequest = convert(json);
        List<String> queries = new ArrayList<>();
        for (QueryRequest.Series series : queryRequest.getSeries()) {
            queries.add(series.getQuery().getName());
        }
        if (queries.size() > 1) {
            throw new UnsupportedOperationException("No implementation for multiple queries");
        }

        for (String query : statsToolProperties.getQueryAccess().keySet()) {
            if (queries.get(0).startsWith(query)) {
                // TODO: check if user has access
                List<StatsToolProperties.Group> groups = statsToolProperties.getQueryAccess().get(query);
                for (StatsToolProperties.Group group : groups) {
                    switch (group.getRole()) {
                        case "Stakeholder":

                            break;
                        case "Coordinator":
                            break;
                    }
                }
            }
        }


        return true;
    }

    QueryRequest convert(String json) {
        try {
            return objectMapper.readValue(json, QueryRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
