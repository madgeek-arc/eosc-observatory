package eu.eosc.observatory.service;

import gr.athenarc.authorization.domain.AuthTriple;
import gr.athenarc.authorization.repository.AuthRepository;
import gr.athenarc.authorization.service.AuthorizationService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthorizationServiceBridge implements PermissionService {

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
        List<String> permissions = Arrays.asList(
                Permissions.READ.getKey(),
                Permissions.WRITE.getKey(),
                Permissions.MANAGE.getKey(),
                Permissions.PUBLISH.getKey());
        return addPermissions(users, permissions, resourceIds);
    }

    @Override
    public Set<AuthTriple> addContributors(List<String> users, List<String> resourceIds) {
        List<String> permissions = Arrays.asList(Permissions.READ.getKey(), Permissions.WRITE.getKey());
        return addPermissions(users, permissions, resourceIds);
    }

    @Override
    public Set<AuthTriple> addPermissions(List<String> users, List<String> actions, List<String> resourceIds) {
        Set<AuthTriple> triples = new HashSet<>();
        for (String id : users) {
            for (String action : actions) {
                for (String resourceId : resourceIds) {
                    if (authRepository.findAllBySubjectAndActionAndObject(id, action, resourceId).isEmpty()) {
                        triples.add(new AuthTriple(id, action, resourceId));
                    }
                }
            }
        }
        authRepository.saveAll(triples);
        return triples;
    }

    @Override
    public void removePermissions(List<String> users, List<String> actions, List<String> resourceIds) {
        for (String id : users) {
            for (String action : actions) {
                for (String resourceId : resourceIds) {
                    Set<AuthTriple> permissions = authRepository.findAllBySubjectAndActionAndObject(id, action, resourceId);
                    for (AuthTriple triple : permissions) {
                        authRepository.delete(triple);
                    }
                }
            }
        }
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
        authRepository.deleteAllBySubjectAndActionAndObject(user, action, resourceId);
    }

    @Override
    public boolean hasPermission(String user, String action, String resourceId) {
        return authorizationService.canDo(user, action, resourceId);
    }

    @Override
    public boolean canRead(String userId, String resourceId) {
        return authorizationService.canDo(userId, Permissions.READ.getKey(), resourceId);
    }

    @Override
    public boolean canWrite(String userId, String resourceId) {
        return authorizationService.canDo(userId, Permissions.WRITE.getKey(), resourceId);
    }

    @Override
    public boolean canManage(String userId, String resourceId) {
        return authorizationService.canDo(userId, Permissions.MANAGE.getKey(), resourceId);
    }

    @Override
    public boolean canPublish(String userId, String resourceId) {
        return authorizationService.canDo(userId, Permissions.PUBLISH.getKey(), resourceId);
    }
}
