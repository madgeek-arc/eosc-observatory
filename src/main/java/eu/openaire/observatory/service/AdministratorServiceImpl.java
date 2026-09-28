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

import eu.openaire.observatory.domain.Administrator;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.permissions.Groups;
import eu.openaire.observatory.permissions.PermissionService;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class AdministratorServiceImpl extends AbstractPermissionedGroupService<Administrator> implements AdministratorService {

    private static final Logger logger = LoggerFactory.getLogger(AdministratorServiceImpl.class);

    private static final String RESOURCE_TYPE = "administrator";

    public AdministratorServiceImpl(ResourceTypeService resourceTypeService,
                                    ResourceService resourceService,
                                    SearchService searchService,
                                    VersionService versionService,
                                    ParserService parserService,
                                    UserService userService,
                                    @Lazy CrudService<SurveyAnswer> surveyAnswerCrudService,
                                    PermissionService permissionService,
                                    ModelResponseValidator validator) {
        super(userService, resourceTypeService, resourceService, searchService, versionService, parserService, surveyAnswerCrudService, permissionService, validator);
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    protected Groups getGroup() {
        return Groups.ADMINISTRATOR;
    }

    @Override
    public String createId(Administrator administrator) {
        String id = String.format("admin-%s", administrator.getType());
        logger.debug("Created new ID for Administrator: {}", id);
        return id;
    }
}
