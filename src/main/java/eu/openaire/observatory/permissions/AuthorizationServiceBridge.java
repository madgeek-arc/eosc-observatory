/*
 * Copyright 2021-2026 OpenAIRE AMKE
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

package eu.openaire.observatory.permissions;

import eu.openaire.observatory.dto.ResourcePermissions;
import eu.openaire.observatory.utils.UserIds;
import gr.uoa.di.madgik.authorization.domain.Permission;
import gr.uoa.di.madgik.authorization.repository.PermissionRepository;
import gr.uoa.di.madgik.authorization.service.Authorization;
import gr.uoa.di.madgik.authorization.service.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthorizationServiceBridge implements PermissionService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationServiceBridge.class);

    private final Authorization authorizationService;
    private final PermissionRepository permissionRepository;

    public AuthorizationServiceBridge(Authorization authorizationService,
                                      PermissionRepository permissionRepository) {
        this.authorizationService = authorizationService;
        this.permissionRepository = permissionRepository;
    }

    /**
     * A permission's subject is a user id, and every repository lookup below matches it exactly
     * ({@code findAllBySubject...}, {@code deleteAllBySubject}). Normalizing here rather than at each
     * caller keeps writes, reads and deletes agreeing by construction — the group services pass ids
     * straight through from request bodies, so a non-canonical one would otherwise be written and
     * then be invisible to both authorization checks and the erasure sweep in
     * {@code UserServiceImpl#purge}.
     */
    private static List<String> normalizeSubjects(Collection<String> users) {
        return users == null ? List.of() : users.stream().map(UserIds::normalize).toList();
    }

    @Override
    public Set<String> getPermissions(String userId, String resourceId) {
        String subject = UserIds.normalize(userId);
        Set<String> permissions = this.authorizationService.whatCan(subject, resourceId)
                .stream().map(Permission::getAction).collect(Collectors.toSet());
        if (logger.isDebugEnabled()) {
            logger.debug("[user: {}] permissions for resource [resourceId: {}]: [permissions: {}]",
                    subject, resourceId, String.join(", ", permissions));
        }
        return permissions;
    }

    @Override
    public Set<Permission> getUserPermissionsByAction(String userId, String action) {
        return this.authorizationService.whereCan(UserIds.normalize(userId), action);
    }

    @Override
    public List<ResourcePermissions> getResourcePermissions(String userId, Collection<String> resourceIds) {
        List<ResourcePermissions> resourcePermissionsList = new ArrayList<>();
        for (String id : resourceIds) {
            ResourcePermissions resourcePermissions = new ResourcePermissions(id, getPermissions(userId, id));
            resourcePermissionsList.add(resourcePermissions);
        }
        return resourcePermissionsList;
    }

    @Override
    public Set<Permission> addPermissions(Collection<String> users, Collection<String> actions, Collection<String> resourceIds, String group) {
        Set<Permission> permissions = new HashSet<>();
        if (users != null && actions != null && resourceIds != null) {
            for (String id : normalizeSubjects(users)) {
                for (String action : actions) {
                    for (String resourceId : resourceIds) {
                        if (permissionRepository.findAllBySubjectAndActionAndObject(id, action, resourceId).isEmpty()) {
                            permissions.add(new Permission(id, action, resourceId, group));
                        }
                    }
                }
            }
        }
        permissionRepository.saveAll(permissions);
        return permissions;
    }

    @Override
    public void removePermissions(Collection<String> users, Collection<String> actions, Collection<String> resourceIds) {
        if (users != null && actions != null && resourceIds != null) {
            for (String id : normalizeSubjects(users)) {
                for (String action : actions) {
                    for (String resourceId : resourceIds) {
                        Set<Permission> permissions = permissionRepository.findAllBySubjectAndActionAndObject(id, action, resourceId);
                        for (Permission permission : permissions) {
                            logger.debug("Deleting permission: {}", permission);
                            permissionRepository.delete(permission);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void removePermissions(Collection<String> users, Collection<String> actions, Collection<String> resourceIds, String group) {
        if (users != null && actions != null && resourceIds != null) {
            StringBuilder deletedPermissions = new StringBuilder();
            for (String id : normalizeSubjects(users)) {
                for (String action : actions) {
                    for (String resourceId : resourceIds) {
                        Set<Permission> permissions = permissionRepository.findAllBySubjectAndActionAndObjectAndSubjectGroup(id, action, resourceId, group);
                        for (Permission permission : permissions) {
                            deletedPermissions.append(String.format("%n%s", permission));
                            permissionRepository.delete(permission);
                        }
                    }
                }
            }
            logger.debug("Deleted permissions: {}", deletedPermissions);
        }
    }

    @Override
    public void removeAll(String user) {
        permissionRepository.deleteAllBySubject(UserIds.normalize(user));
    }

    @Override
    public void removeAll(String user, String group) {
        permissionRepository.deleteAllBySubjectAndSubjectGroup(UserIds.normalize(user), group);
    }

    @Override
    public void removeAll(Collection<String> users) {
        if (users != null) {
            for (String user : users) {
                this.removeAll(user);
            }
        }
    }

    @Override
    public void removeAll(Collection<String> users, String group) {
        if (users != null) {
            for (String user : users) {
                this.removeAll(user, group);
            }
        }
    }

    @Override
    public void remove(String user, String action, String resourceId) {
        permissionRepository.deleteAllBySubjectAndActionAndObject(UserIds.normalize(user), action, resourceId);
    }

    @Override
    public boolean hasPermission(String user, String action, String resourceId) {
        return authorizationService.canDo(UserIds.normalize(user), action, resourceId);
    }

    @Override
    public boolean canRead(String userId, String resourceId) {
        return authorizationService.canDo(UserIds.normalize(userId), Permissions.READ.getKey(), resourceId);
    }

    @Override
    public boolean canWrite(String userId, String resourceId) {
        return authorizationService.canDo(UserIds.normalize(userId), Permissions.WRITE.getKey(), resourceId);
    }

    @Override
    public boolean canManage(String userId, String resourceId) {
        return authorizationService.canDo(UserIds.normalize(userId), Permissions.MANAGE.getKey(), resourceId);
    }

    @Override
    public boolean canPublish(String userId, String resourceId) {
        return authorizationService.canDo(UserIds.normalize(userId), Permissions.PUBLISH.getKey(), resourceId);
    }
}
