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
package eu.eosc.observatory.aspect;

import eu.eosc.observatory.domain.Coordinator;
import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.domain.UserGroup;
import eu.eosc.observatory.permissions.Groups;
import eu.eosc.observatory.permissions.PermissionService;
import eu.eosc.observatory.permissions.Permissions;
import eu.eosc.observatory.service.CoordinatorService;
import eu.eosc.observatory.service.StakeholderService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Aspect
@Component
public class PermissionsUpdateAspect {

    private static final Logger logger = LoggerFactory.getLogger(PermissionsUpdateAspect.class);

    private final PermissionService permissionService;
    private final StakeholderService stakeholderService;
    private final CoordinatorService coordinatorService;

    @Autowired
    public PermissionsUpdateAspect(PermissionService permissionService,
                                   StakeholderService stakeholderService,
                                   CoordinatorService coordinatorService) {
        this.permissionService = permissionService;
        this.stakeholderService = stakeholderService;
        this.coordinatorService = coordinatorService;
    }

    @AfterReturning(value = "execution(* eu.eosc.observatory.service.SurveyAnswerCrudService.add(..))", returning = "surveyAnswer")
    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 5000))
    public void onAddSurveyAnswer(JoinPoint joinPoint, SurveyAnswer surveyAnswer) {
        logger.info("Adding permissions for SurveyAnswer with [id: {}]", surveyAnswer.getId());

        // adds Stakeholder permissions
        Stakeholder stakeholder = stakeholderService.get(surveyAnswer.getStakeholderId());
        List<String> managerPermissions = Arrays.asList(
                Permissions.READ.getKey(),
                Permissions.WRITE.getKey(),
                Permissions.MANAGE.getKey(),
                Permissions.PUBLISH.getKey());

        List<String> contributorPermissions = Arrays.asList(
                Permissions.READ.getKey(),
                Permissions.WRITE.getKey());

        List<String> resourceIds = new ArrayList<>();
        resourceIds.add(surveyAnswer.getId());
        permissionService.addPermissions(stakeholder.getAdmins(), managerPermissions, resourceIds, Groups.STAKEHOLDER_MANAGER.getKey());
        permissionService.addPermissions(stakeholder.getMembers(), contributorPermissions, resourceIds, Groups.STAKEHOLDER_CONTRIBUTOR.getKey());

        // adds permissions to associated Coordinators
        Set<Coordinator> coordinators = coordinatorService.getWithFilter("type", surveyAnswer.getType());
        Set<String> coordinatorMembers = coordinators.stream()
                .map(UserGroup::getUsers)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        permissionService.addPermissions(coordinatorMembers, Collections.singletonList(Permissions.READ.getKey()), resourceIds, Groups.COORDINATOR.getKey());
    }

    @AfterReturning(value = "execution(* eu.eosc.observatory.service.SurveyAnswerCrudService.delete(..))", returning = "surveyAnswer")
    public void onDeleteSurveyAnswer(JoinPoint joinPoint, SurveyAnswer surveyAnswer) {
        logger.info("Deleting permissions for SurveyAnswer with [id: {}]", surveyAnswer.getId());

        // removes Stakeholder permissions
        Stakeholder stakeholder = stakeholderService.get(surveyAnswer.getStakeholderId());
        List<String> permissions = Arrays.asList(
                Permissions.READ.getKey(),
                Permissions.WRITE.getKey(),
                Permissions.MANAGE.getKey(),
                Permissions.PUBLISH.getKey());

        List<String> resourceIds = new ArrayList<>();
        resourceIds.add(surveyAnswer.getId());

        permissionService.removePermissions(stakeholder.getAdmins(), permissions, resourceIds, Groups.STAKEHOLDER_MANAGER.getKey());
        permissionService.removePermissions(stakeholder.getMembers(), permissions, resourceIds, Groups.STAKEHOLDER_CONTRIBUTOR.getKey());

        // removes permissions from associated Coordinators
        Set<Coordinator> coordinators = coordinatorService.getWithFilter("type", surveyAnswer.getType());
        Set<String> coordinatorMembers = coordinators.stream()
                .map(UserGroup::getUsers)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        permissionService.removePermissions(coordinatorMembers, permissions, resourceIds, Groups.COORDINATOR.getKey());
    }
}
