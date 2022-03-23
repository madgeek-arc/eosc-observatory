package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.SurveyAnswer;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.catalogue.service.id.IdGenerator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SurveyAnswerCrudService extends AbstractCrudItemService<SurveyAnswer> implements CrudItemService<SurveyAnswer> {

    private static final Logger logger = LogManager.getLogger(SurveyAnswerCrudService.class);

    private final IdGenerator<String> idGenerator;

    @Autowired
    public SurveyAnswerCrudService(ResourceTypeService resourceTypeService,
                                   ResourceService resourceService,
                                   SearchService searchService,
                                   ParserService parserService,
                                   IdGenerator<String> idGenerator) {
        super(resourceTypeService, resourceService, searchService, parserService);
        this.idGenerator = idGenerator;
    }

    @Override
    public String createId(SurveyAnswer resource) {
        return idGenerator.createId("sa-", 8);
    }

    @Override
    public String getResourceType() {
        return "survey_answer";
    }

    @Override // only exists to get caught from permissions aspect
    public SurveyAnswer add(SurveyAnswer surveyAnswer) {
        return super.add(surveyAnswer);
    }

    @Override // only exists to get caught from permissions aspect
    public SurveyAnswer delete(String id) {
        return super.delete(id);
    }
}
