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

package eu.openaire.observatory.service;

import eu.openaire.observatory.configuration.ApplicationProperties;
import eu.openaire.observatory.domain.*;
import eu.openaire.observatory.permissions.PermissionService;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.*;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserServiceImpl extends AbstractCrudService<User> implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    public static final String DELETED_USER_PLACEHOLDER = "[Deleted User]";

    private final PrivacyPolicyService privacyPolicyService;
    private final CrudService<Stakeholder> stakeholderCrudService;
    private final CrudService<Coordinator> coordinatorCrudService;
    private final CrudService<Administrator> administratorCrudService;
    private final StakeholderService stakeholderService;
    private final CoordinatorService coordinatorService;
    private final AdministratorService administratorService;
    private final CrudService<SurveyAnswer> surveyAnswerCrudService;
    private final PermissionService permissionService;
    private final SurveyAnswerCommentService commentService;
    private final ApplicationProperties applicationProperties;

    protected UserServiceImpl(ResourceTypeService resourceTypeService,
                              ResourceService resourceService,
                              SearchService searchService,
                              VersionService versionService,
                              ParserService parserService,
                              PrivacyPolicyService privacyPolicyService,
                              @Lazy CrudService<Stakeholder> stakeholderCrudService,
                              @Lazy CrudService<Coordinator> coordinatorCrudService,
                              @Lazy CrudService<Administrator> administratorCrudService,
                              @Lazy StakeholderService stakeholderService,
                              @Lazy CoordinatorService coordinatorService,
                              @Lazy AdministratorService administratorService,
                              @Lazy CrudService<SurveyAnswer> surveyAnswerCrudService,
                              PermissionService permissionService,
                              SurveyAnswerCommentService commentService,
                              ApplicationProperties applicationProperties,
                              ModelResponseValidator validator) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService, validator);
        this.privacyPolicyService = privacyPolicyService;
        this.stakeholderCrudService = stakeholderCrudService;
        this.coordinatorCrudService = coordinatorCrudService;
        this.administratorCrudService = administratorCrudService;
        this.stakeholderService = stakeholderService;
        this.coordinatorService = coordinatorService;
        this.administratorService = administratorService;
        this.surveyAnswerCrudService = surveyAnswerCrudService;
        this.permissionService = permissionService;
        this.commentService = commentService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public String createId(User resource) {
        return resource.getEmail();
    }

    @Override
    public String getResourceType() {
        return "user";
    }

    @Override
    public User getUser(String id) {
        User user = null;
        try {
            user = get(id);
        } catch (ResourceNotFoundException e) {
            logger.debug("User not found in DB");
            user = new User();
            user.setEmail(id);
        }
        return user;
    }

    @Override
    public UserInfo getUserInfo(String userId) {
        User user = getUser(userId);
        return createUserInfo(user);
    }

    @Override
    public UserInfo getUserInfo(Authentication authentication) {
        User user;
        try {
            user = get(User.getId(authentication));
        } catch (ResourceNotFoundException e) {
            logger.debug("User not found in DB");
            user = User.of(authentication);
        }
        return createUserInfo(user);
    }

    @Override
    public User acceptPrivacyPolicy(String policyId, Authentication authentication) {
        User user = User.of(authentication);
        PrivacyPolicy policy = privacyPolicyService.get(policyId);

        FacetFilter filter = new FacetFilter();
        filter.addFilter("policyId", policy.getId());
        filter.addFilter("resource_internal_id", User.of(authentication).getId());
        Browsing<User> userBrowsing = getAll(filter);
        if (userBrowsing.getTotal() == 1) {
            //
            user = userBrowsing.getResults().get(0);

        } else if (userBrowsing.getTotal() > 1) {
            logger.error(String.format("More than one user with [id=%s] was found.", user.getId()));
            user = null;
        } else {
            //
            user = get(user.getId());
            if (user.getPoliciesAccepted() == null) {
                user.setPoliciesAccepted(new ArrayList<>());
            }
            user.getPoliciesAccepted().add(new PolicyAccepted(policy.getId(), new Date().getTime()));
            user = update(user.getId(), user);
        }

        return user;
    }

    @Override
    public void updateUserDetails(Authentication authentication) {
        User user = User.of(authentication);
        try {
            User existing = this.get(user.getId());
            if (!existing.getFullname().equals(user.getFullname())) {
                existing.setName(user.getName());
                existing.setSurname(user.getSurname());
                existing.setFullname(user.getFullname());
                this.update(existing.getId(), existing); // save previous user object with updated name/surname
            }
            if (!existing.getSub().equals(user.getSub())) {
                logger.warn("User sub has been changed: '{}' -> '{}'", existing.getSub(), user.getSub());
                existing.setSub(user.getSub());
                this.update(existing.getId(), existing); // save previous user object with updated sub
            }
        } catch (ResourceNotFoundException e) {
            logger.debug(String.format("User not found! Adding User to database [user=%s]", user));
            this.add(user);
        }
    }

    @Override
    public void purge(String id) throws ResourceNotFoundException {
        // Remove from all stakeholder groups (handles permission cleanup internally)
        Set<Stakeholder> stakeholders = stakeholderCrudService.getWithFilter("users", id);
        for (Stakeholder s : stakeholders) {
            stakeholderService.removeMember(s.getId(), id);
            stakeholderService.removeAdmin(s.getId(), id);
        }

        // Remove from all coordinator groups
        Set<Coordinator> coordinators = coordinatorCrudService.getWithFilter("users", id);
        for (Coordinator c : coordinators) {
            coordinatorService.removeMember(c.getId(), id);
            coordinatorService.removeAdmin(c.getId(), id);
        }

        // Remove from all administrator groups
        Set<Administrator> administrators = administratorCrudService.getWithFilter("users", id);
        for (Administrator a : administrators) {
            administratorService.removeMember(a.getId(), id);
        }

        // Anonymize user identity from all survey answer history and metadata
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        List<SurveyAnswer> surveyAnswers = surveyAnswerCrudService.getAll(filter).getResults();
        for (SurveyAnswer answer : surveyAnswers) {
            boolean modified = false;
            if (answer.getHistory() != null && answer.getHistory().getEntries() != null) {
                for (HistoryEntry entry : answer.getHistory().getEntries()) {
                    if (entry.getEditors() != null) {
                        for (Editor editor : entry.getEditors()) {
                            if (id.equals(editor.getUser())) {
                                editor.setUser(DELETED_USER_PLACEHOLDER);
                                modified = true;
                            }
                        }
                    }
                }
            }
            if (answer.getMetadata() != null) {
                if (id.equals(answer.getMetadata().getCreatedBy())) {
                    answer.getMetadata().setCreatedBy(DELETED_USER_PLACEHOLDER);
                    modified = true;
                }
                if (id.equals(answer.getMetadata().getModifiedBy())) {
                    answer.getMetadata().setModifiedBy(DELETED_USER_PLACEHOLDER);
                    modified = true;
                }
            }
            if (modified) {
                surveyAnswerCrudService.update(answer.getId(), answer);
            }
        }

        // Anonymize comment authorship and @mentions in survey comments
        commentService.anonymizeUser(id, DELETED_USER_PLACEHOLDER);

        // Safety net: remove any remaining permissions
        permissionService.removeAll(id);

        // Delete the user record
//        User user = delete(id);  // old: only removed the user record from DB
        delete(id);

        // Note: registry versioned copies (core versions) are intentionally left for manual cleanup
    }

    private UserInfo createUserInfo(User user) {
        UserInfo info = new UserInfo();
        info.setUser(user);
        info.setAdmin(applicationProperties.getAdmins().contains(user.getEmail()));
        info.setStakeholders(new HashSet<>());
        info.setCoordinators(new HashSet<>());
        info.setAdministrators(new HashSet<>());

        info.getStakeholders().addAll(stakeholderCrudService.getWithFilter("users", user.getId()));
        info.getCoordinators().addAll(coordinatorCrudService.getWithFilter("users", user.getId()));
        info.getAdministrators().addAll(administratorCrudService.getWithFilter("users", user.getId()));
        return info;
    }
}
