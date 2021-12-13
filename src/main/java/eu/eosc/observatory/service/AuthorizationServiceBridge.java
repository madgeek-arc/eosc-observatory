package eu.eosc.observatory.service;

import gr.athenarc.authorization.domain.AuthTriple;
import gr.athenarc.authorization.repository.AuthRepository;
import gr.athenarc.authorization.service.AuthorizationService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthorizationServiceBridge implements PermissionsService {

    private static final Logger logger = LogManager.getLogger(AuthorizationServiceBridge.class);

    private final AuthorizationService authorizationService;
    private final AuthRepository authRepository;

    public AuthorizationServiceBridge(AuthorizationService authorizationService,
                                      AuthRepository authRepository) {
        this.authorizationService = authorizationService;
        this.authRepository = authRepository;
    }

    @Override
    public Set<String> getPermissions(String userId, String resourceId) {
        Set<String> permissions = this.authorizationService.whatCan(userId, resourceId)
                .stream().map(AuthTriple::getAction).collect(Collectors.toSet());
        logger.debug(String.format("[user: %s] permissions for resource [resourceId: %s]: [permissions: %s]", userId, resourceId, String.join(", ", permissions)));
        return permissions;
    }

    @Override
    public Set<AuthTriple> addManagers(List<String> users, List<String> resourceIds) {
        // TODO Refactor here .... probably create enum with permissions
        List<String> permissions = Arrays.asList("read", "write", "manage", "publish");
        return addPermissions(users, permissions, resourceIds);
    }

    @Override
    public Set<AuthTriple> addContributors(List<String> users, List<String> resourceIds) {
        // TODO Refactor here .... probably create enum with permissions
        List<String> permissions = Arrays.asList("read", "write");
        return addPermissions(users, permissions, resourceIds);
    }

    @Override
    public Set<AuthTriple> addPermissions(List<String> users, List<String> actions, List<String> resourceIds) {
        Set<AuthTriple> triples = new HashSet<>();
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

    @Override
    public void removeAll(String user) {
        authRepository.deleteAllBySubject(user);
    }

    @Override
    public void removeAll(List<String> users) {
        for (String user : users) {
            this.removeAll(user);
        }
    }

    @Override
    public void remove(String user, String action, String resourceId) {
        authRepository.deleteBySubjectAndActionAndObject(user, action, resourceId);
    }

    @Override
    public boolean hasPermission(String user, String action, String resourceId) {
        return authorizationService.canDo(user, action, resourceId);
    }

    @Override
    public boolean canRead(String userId, String resourceId) {
        return authorizationService.canDo(userId, "read", resourceId);
    }

    @Override
    public boolean canWrite(String userId, String resourceId) {
        return authorizationService.canDo(userId, "write", resourceId);
    }

    @Override
    public boolean canValidate(String userId, String resourceId) {
        return  authorizationService.canDo(userId, "validate", resourceId);
    }

    @Override
    public boolean canManage(String userId, String resourceId) {
        return  authorizationService.canDo(userId, "manage", resourceId);
    }
}
