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
package eu.openaire.observatory.permissions;

import eu.openaire.observatory.dto.ResourcePermissions;
import gr.uoa.di.madgik.authorization.domain.Permission;

import java.util.Collection;
import java.util.Set;

public interface PermissionService {

    Set<String> getPermissions(String userId, String resourceId);

    Set<Permission> getUserPermissionsByAction(String userId, String action);

    Collection<ResourcePermissions> getResourcePermissions(String userId, Collection<String> resourceIds);

    Set<Permission> addPermissions(Collection<String> users, Collection<String> actions, Collection<String> resourceIds, String group);

    void removePermissions(Collection<String> users, Collection<String> actions, Collection<String> resourceIds);

    void removePermissions(Collection<String> users, Collection<String> actions, Collection<String> resourceIds, String group);

    void removeAll(String user);

    void removeAll(String user, String group);

    void removeAll(Collection<String> users);

    void removeAll(Collection<String> users, String group);

    void remove(String user, String action, String resourceId);

    boolean hasPermission(String user, String action, String resourceId);

    boolean canRead(String userId, String resourceId);

    boolean canWrite(String userId, String resourceId);

    boolean canManage(String userId, String resourceId);

    boolean canPublish(String userId, String resourceId);
}
