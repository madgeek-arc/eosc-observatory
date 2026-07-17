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
                              @Lazy SurveyAnswerCommentService commentService,
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

    /**
     * Not wrapped in a single transaction: the steps span both the registry's
     * Postgres+Elasticsearch-backed group/permission storage and this service's own
     * JPA-backed comment/user storage, so no single transaction manager could cover all of it.
     * Instead, every step here is idempotent (group/permission removal and the placeholder
     * rewrites are no-ops when reapplied), so on partial failure it is safe to simply call
     * purge() again — it will pick up wherever it left off. The one exception is the final
     * delete(id): if a prior run already completed, retrying throws ResourceNotFoundException,
     * which is the expected/idiomatic response for deleting an already-deleted resource.
     */
    @Override
    public void purge(String id) throws ResourceNotFoundException {
        // Normalize to lowercase up front: emails/ids are stored lowercase everywhere
        // (see User#setEmail, User#getId), but this id comes from a path variable and
        // isn't guaranteed to match that casing. Every comparison/query below is exact-case.
        id = id.toLowerCase();

        // TODO: removeMember/removeAdmin below only clear this user from the *current* version
        // of each Stakeholder. Registry core versions still show this user in past members/admins
        // sets. Implement user erasure from Stakeholder and its versions.
        // Remove from all stakeholder groups (handles permission cleanup internally)
        Set<Stakeholder> stakeholders = stakeholderCrudService.getWithFilter("users", id);
        List<String> stakeholderIds = new ArrayList<>();
        for (Stakeholder s : stakeholders) {
            stakeholderService.removeMember(s.getId(), id);
            stakeholderService.removeAdmin(s.getId(), id);
            stakeholderIds.add(s.getId());
        }

        // TODO: same gap as above, for Coordinator — past members/admins sets survive in its
        // registry core versions. Implement user erasure from Coordinator and its versions.
        // Remove from all coordinator groups
        Set<Coordinator> coordinators = coordinatorCrudService.getWithFilter("users", id);
        List<String> coordinatorIds = new ArrayList<>();
        for (Coordinator c : coordinators) {
            coordinatorService.removeMember(c.getId(), id);
            coordinatorService.removeAdmin(c.getId(), id);
            coordinatorIds.add(c.getId());
        }

        // TODO: same gap as above, for Administrator — past members sets survive in its
        // registry core versions. Implement user erasure from Administrator and its versions.
        // Remove from all administrator groups
        Set<Administrator> administrators = administratorCrudService.getWithFilter("users", id);
        List<String> administratorIds = new ArrayList<>();
        for (Administrator a : administrators) {
            administratorService.removeMember(a.getId(), id);
            administratorIds.add(a.getId());
        }

        // TODO: the anonymization below only rewrites the *current* version of each SurveyAnswer.
        // Registry core versions still show this user as editor/creator/modifier in past
        // revisions. Implement user erasure from Surveys and their versions.
        // Anonymize user identity from all survey answer history and metadata.
        // 10000 is Elasticsearch's default max result window, not an arbitrary cap; a single
        // user participating in more than that many surveys is not a realistic scenario.
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        List<SurveyAnswer> surveyAnswers = surveyAnswerCrudService.getAll(filter).getResults();
        int surveyAnswersAnonymized = 0;
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
                surveyAnswersAnonymized++;
            }
        }

        // Anonymize comment authorship and @mentions in survey comments
        commentService.anonymizeUser(id, DELETED_USER_PLACEHOLDER);

        // Safety net: remove any remaining permissions
        permissionService.removeAll(id);

        // Report what the purge touched, for audit/compliance purposes. Logged before the
        // final delete() so the report is captured even if that last step fails.
        // Deliberately omits the purged user's id/email from the log line — logging the
        // identifier being purged would itself retain the PII this method exists to remove.
        logger.info("Purge report: removed from stakeholder group(s) {}, coordinator group(s) {}, " +
                        "administrator group(s) {}; anonymized {} survey answer(s); anonymized comment authorship " +
                        "and @mentions; removed residual permissions.",
                stakeholderIds, coordinatorIds, administratorIds, surveyAnswersAnonymized);

        // TODO: delete(id) below only removes the *current* User resource. Registry core
        // versions still retain this user's full PII (name, email, etc.) from before this purge.
        // Implement erasure of the User resource's own version history too.
        // Delete the user record
        delete(id);
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
