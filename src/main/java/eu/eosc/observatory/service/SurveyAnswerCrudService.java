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
package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.domain.SurveyAnswerRevisionsAggregation;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.*;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.service.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SurveyAnswerCrudService extends AbstractCrudService<SurveyAnswer> implements CrudService<SurveyAnswer> {

    private static final Logger logger = LoggerFactory.getLogger(SurveyAnswerCrudService.class);

    private final IdGenerator<String> idGenerator;
    private final CacheService<String, SurveyAnswerRevisionsAggregation> cacheService;

    public SurveyAnswerCrudService(ResourceTypeService resourceTypeService,
                                   ResourceService resourceService,
                                   SearchService searchService,
                                   VersionService versionService,
                                   ParserService parserService,
                                   IdGenerator<String> idGenerator,
                                   CacheService<String, SurveyAnswerRevisionsAggregation> cacheService) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService);
        this.idGenerator = idGenerator;
        this.cacheService = cacheService;
    }


    @Scheduled(fixedDelay = 60_000)
    void autoSaveCache() throws gr.uoa.di.madgik.registry.exception.ResourceNotFoundException {
        Set<String> keys = cacheService.fetchKeys("sa-*");
        for (String key : keys) {
            SurveyAnswerRevisionsAggregation sara = cacheService.fetch(key);
            if (sara != null) {
                long active = new Date().getTime() - sara.getCreated().getTime();
                if (active > 600_000) {
                    cacheService.remove(key);
                    update(sara.getSurveyAnswer().getId(), sara.getSurveyAnswer());
                }
            }
        }
    }


    @Override
    public String createId(SurveyAnswer resource) {
        return idGenerator.createId("sa-", 8);
    }

    @Override
    public String getResourceType() {
        return "survey_answer";
    }

    @Override // DO NOT REMOVE: needed to get caught from permissions aspect
    public SurveyAnswer add(SurveyAnswer surveyAnswer) {
        return super.add(surveyAnswer);
    }


    /*
     * Overridden methods to check cache and fetch most recent resources.
     */

    @Override // DO NOT REMOVE: needed to get caught from permissions aspect
    public SurveyAnswer delete(String id) {
        cacheService.remove(id);
        return super.delete(id);
    }

    @Override
    public SurveyAnswer get(String id) {
        SurveyAnswer answer;
        if (cacheService.containsKey(id)) {
            answer = cacheService.fetch(id).getSurveyAnswer();
        } else {
            answer = super.get(id);
        }
        return answer;
    }

    @Override
    public Browsing<SurveyAnswer> getAll(FacetFilter filter) {
        Browsing<SurveyAnswer> browsing = super.getAll(filter);
        browsing.setResults(browsing.getResults()
                .stream()
                .map(a -> {
                    if (cacheService.containsKey(a.getId())) {
                        return cacheService.fetch(a.getId()).getSurveyAnswer();
                    }
                    return a;
                })
                .toList());
        return browsing;
    }

    @Override
    public Browsing<SurveyAnswer> getMy(FacetFilter filter) {
        Browsing<SurveyAnswer> browsing = super.getMy(filter);
        browsing.setResults(browsing.getResults()
                .stream()
                .map(a -> {
                    if (cacheService.containsKey(a.getId())) {
                        return cacheService.fetch(a.getId()).getSurveyAnswer();
                    }
                    return a;
                })
                .toList());
        return browsing;
    }

    @Override
    public Set<SurveyAnswer> getWithFilter(String key, String value) {
        return super.getWithFilter(key, value)
                .stream()
                .map(a -> {
                    if (cacheService.containsKey(a.getId())) {
                        return cacheService.fetch(a.getId()).getSurveyAnswer();
                    }
                    return a;
                })
                .collect(Collectors.toSet());
    }

    @Override
    public SurveyAnswer update(String id, SurveyAnswer resource) throws ResourceNotFoundException {
        if (cacheService.containsKey(id)) {
            super.update(id, cacheService.remove(id).getSurveyAnswer());
        }
        return super.update(id, resource);
    }

    @Override
    public Resource getResource(String id) {
        // TODO: force update if found in cache and then return??
        return super.getResource(id);
    }
}
