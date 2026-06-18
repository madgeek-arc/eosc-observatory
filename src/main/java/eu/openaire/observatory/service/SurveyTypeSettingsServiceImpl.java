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

import eu.openaire.observatory.domain.SurveyTypeSettings;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SurveyTypeSettingsServiceImpl extends AbstractCrudService<SurveyTypeSettings> implements SurveyTypeSettingsService {

    protected SurveyTypeSettingsServiceImpl(ResourceTypeService resourceTypeService,
                                            ResourceService resourceService,
                                            SearchService searchService,
                                            VersionService versionService,
                                            ParserService parserService,
                                            ModelResponseValidator validator) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService, validator);
    }

    @Override
    public String createId(SurveyTypeSettings resource) {
        return "survey-type-settings-" + resource.getSurveyType();
    }

    @Override
    public String getResourceType() {
        return "survey_type_settings";
    }

    @Override
    public SurveyTypeSettings getByType(String surveyType) {
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(1);
        filter.addFilter("surveyType", surveyType);
        Browsing<SurveyTypeSettings> results = getAll(filter);
        List<SurveyTypeSettings> list = results.getResults();
        return list.isEmpty() ? null : list.get(0);
    }
}
