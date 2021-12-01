package eu.eosc.observatory.service;

import gr.athenarc.authorization.domain.AuthTriple;
import gr.athenarc.authorization.repository.AuthRepository;
import gr.athenarc.authorization.service.AuthorizationService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResourcePermissionsService implements PermissionsService {

    private static final Logger logger = LogManager.getLogger(ResourcePermissionsService.class);

    private final AuthorizationService authorizationService;
    private final AuthRepository authRepository;

    public ResourcePermissionsService(AuthorizationService authorizationService,
                                      AuthRepository authRepository) {
        this.authorizationService = authorizationService;
        this.authRepository = authRepository;
    }

    @Override
    public List<String> getPermissions(String userId, String resourceId) {
        List<String> permissions = this.authorizationService.whatCan(userId, resourceId)
                .stream().map(AuthTriple::getAction).collect(Collectors.toList());
        logger.debug(String.format("[user: %s] permissions for resource [resourceId: %s]: [permissions: %s]", userId, resourceId, String.join(", ", permissions)));
        return permissions;
    }

    @Override
    public List<AuthTriple> addManagers(List<String> users, List<String> resourceIds) {
        // TODO Refactor here .... probably create enum with permissions
        List<String> permissions = Arrays.asList("read", "write", "manage", "publish");
        return addPermissions(users, permissions, resourceIds);
    }

    @Override
    public List<AuthTriple> addContributors(List<String> users, List<String> resourceIds) {
        // TODO Refactor here .... probably create enum with permissions
        List<String> permissions = Arrays.asList("read", "write");
        return addPermissions(users, permissions, resourceIds);
    }

    @Override
    public List<AuthTriple> addPermissions(List<String> users, List<String> actions, List<String> resourceIds) {
        List<AuthTriple> triples = new ArrayList<>();
        for (String id : users) {
            for (String action : actions) {
                for (String resourceId : resourceIds) {
                    triples.add(new AuthTriple(id, action, resourceId));
                }
            }
        }
        authRepository.saveAll(triples);
        return triples;
    }
}
