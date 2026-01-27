/*
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
package eu.openaire.observatory.configuration.security;

import eu.openaire.observatory.resources.model.Document;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface MethodSecurityExpressions {

    // UserGroup Methods

    boolean userIsMemberOfGroup(String userId, String groupId);

    boolean userIsMemberOfGroup(String userId, List<String> groupIds);

    // Stakeholder Methods

    boolean userIsStakeholderMember(String userId, String stakeholderId);

    boolean isStakeholderMember(String stakeholderId);

    boolean userIsStakeholderManager(String userId, String stakeholderId);

    boolean isStakeholderManager(String stakeholderId);

    // Coordinator Methods

    boolean userIsCoordinator(String userId, String coordinatorId);

    boolean isCoordinator(String coordinatorId);

    boolean userIsCoordinatorOfType(String userId, String type);

    boolean isCoordinatorOfType(String type);

    boolean userIsCoordinatorOfStakeholder(String userId, String stakehodlerId);

    boolean isCoordinatorOfStakeholder(String stakehodlerId);

    boolean hasStakeholderManagerAccessOnSurvey(String surveyId);

    boolean hasCoordinatorAccessOnSurvey(String surveyId);

    boolean hasStakeholderManagerAccess(Object surveyAnswer);

    boolean hasCoordinatorAccess(Object surveyAnswer);

    // Administrator Methods
    boolean userIsAdministrator(String userId, String administratorId);

    boolean isAdministrator(String administratorId);

    boolean userIsAdministratorOfType(String userId, String type);

    boolean isAdministratorOfCoordinator(String coordinatorId);

    boolean isAdministratorOfStakeholder(String stakehodlerId);

    boolean isAdministratorOfType(String type);

//    boolean userIsAdministratorOfStakeholder(String userId, String stakehodlerId);

    // Extra Methods

    boolean hasAccess(Object resource, Object permission);

    boolean isAdmin(Authentication authentication);

    // Resources Registry Methods

    boolean canReadDocuments(Document.Status status);
    boolean canReadDocument(String documentId);
    boolean canWriteDocument(String documentId);

}
