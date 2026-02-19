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

import eu.openaire.observatory.domain.PrivacyPolicy;
import eu.openaire.observatory.domain.User;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;


@Service
public class UserPrivacyPolicyService extends AbstractCrudService<PrivacyPolicy> implements PrivacyPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(UserPrivacyPolicyService.class);

    private final UserService userService;

    public UserPrivacyPolicyService(ResourceTypeService resourceTypeService,
                                    ResourceService resourceService,
                                    SearchService searchService,
                                    VersionService versionService,
                                    ParserService parserService,
                                    @Lazy UserService userService,
                                    ModelResponseValidator validator) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService, validator);
        this.userService = userService;
    }

    @Override
    public String createId(PrivacyPolicy resource) {
        return resource.getId();
    }

    @Override
    public String getResourceType() {
        return "privacy_policy";
    }

    @Override
    public PrivacyPolicy getLatestByType(String type) {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("type", type);
        filter.addOrderBy("activationDate", "desc");
        Browsing<PrivacyPolicy> policies = getAll(filter);
        return policies.getTotal() > 0 ? policies.getResults().get(0) : null;
    }

    @Override
    public boolean hasAcceptedPolicy(String policyId, String userId) {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("resource_internal_id", userId);
        filter.addFilter("policyId", policyId);
        Browsing<User> users = userService.getAll(filter);
        return users.getTotal() >= 1;
    }
}
