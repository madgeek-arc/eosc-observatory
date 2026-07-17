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

import eu.openaire.observatory.domain.SurveySettings;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SurveySettingsServiceImpl extends AbstractCrudService<SurveySettings> implements SurveySettingsService {

    protected SurveySettingsServiceImpl(ResourceTypeService resourceTypeService,
                                        ResourceService resourceService,
                                        SearchService searchService,
                                        VersionService versionService,
                                        ParserService parserService,
                                        ModelResponseValidator validator) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService, validator);
    }

    @Override
    public String createId(SurveySettings resource) {
        return "survey-settings-" + resource.getSurveyType();
    }

    @Override
    public String getResourceType() {
        return "survey_settings";
    }

    @Override
    public SurveySettings getByType(String surveyType) throws ResourceNotFoundException {
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(1);
        filter.addFilter("surveyType", surveyType);
        Browsing<SurveySettings> results = getAll(filter);
        List<SurveySettings> list = results.getResults();
        if (list.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return list.getFirst();
    }

    @Override
    public SurveySettings upsert(SurveySettings settings) {
        try {
            SurveySettings existing = getByType(settings.getSurveyType());
            settings.setId(existing.getId()); // set ID when updating
            return update(existing.getId(), settings);
        } catch (ResourceNotFoundException e) {
            return add(settings);
        }
    }

    @Override
    public SurveySettings deleteByType(String surveyType) throws ResourceNotFoundException {
        SurveySettings existing = getByType(surveyType);
        if (existing == null) {
            throw new ResourceNotFoundException("No settings found for survey type: " + surveyType);
        }
        return delete(existing.getId());
    }
}
