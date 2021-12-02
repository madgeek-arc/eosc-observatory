package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Metadata;
import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.SurveyAnswer;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.athenarc.catalogue.service.GenericItemService;
import gr.athenarc.catalogue.ui.domain.Survey;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SurveyServiceImpl implements SurveyService {

    private static final Logger logger = LogManager.getLogger(SurveyServiceImpl.class);

    public final CrudItemService<Stakeholder> stakeholderCrudService;
    public final CrudItemService<SurveyAnswer> surveyAnswerCrudService;
    public final GenericItemService genericItemService;
    public final PermissionsService permissionsService;

    @Autowired
    public SurveyServiceImpl(CrudItemService<Stakeholder> stakeholderCrudService,
                             CrudItemService<SurveyAnswer> surveyAnswerCrudService,
                             @Qualifier("catalogueGenericItemService") GenericItemService genericItemService,
                             PermissionsService permissionsService) {
        this.stakeholderCrudService = stakeholderCrudService;
        this.surveyAnswerCrudService = surveyAnswerCrudService;
        this.genericItemService = genericItemService;
        this.permissionsService = permissionsService;
    }

    @Override
    public Browsing<Survey> getByType(FacetFilter filter, String type) {
        filter.setResourceType("survey");
        Browsing<Survey> surveyBrowsing = this.genericItemService.getResults(filter);
        return surveyBrowsing;
    }

    @Override
    public SurveyAnswer getLatest(String surveyId, String stakeholderId) {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("surveyId", surveyId);
        filter.addFilter("stakeholderId", stakeholderId);
        Map<String, Object> sortBy = new HashMap<>();
        Map<String, Object> orderType = new HashMap<>();
        orderType.put("order", "desc");
        sortBy.put("creationDate", orderType);
        filter.setOrderBy(sortBy);

        Browsing<SurveyAnswer> answersBrowsing = surveyAnswerCrudService.getAll(filter);
        SurveyAnswer answer = null;
        if (answersBrowsing.getTotal() > 0) {
            answer = answersBrowsing.getResults().get(0);
        }
        return answer;
    }

    @Override
    public List<SurveyAnswer> createNewCycle(Authentication authentication) {
        logger.debug("Generating new cycle of Survey Answers");
        List<SurveyAnswer> surveyAnswers = new ArrayList<>();
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        List<Stakeholder> stakeholders = this.stakeholderCrudService.getAll(filter).getResults();

        SurveyAnswer surveyAnswer = new SurveyAnswer();
        JSONObject object = new JSONObject();
        surveyAnswer.setAnswer(object);

        Metadata metadata = new Metadata(authentication);
        surveyAnswer.setMetadata(metadata);

        for (Stakeholder stakeholder : stakeholders) {
            List<Survey> surveys = getByType(filter, stakeholder.getType()).getResults();
            surveyAnswer.setStakeholderId(stakeholder.getId());
            for (Survey survey : surveys) {
                surveyAnswer.setSurveyId(survey.getId());
                SurveyAnswer answer = surveyAnswerCrudService.add(surveyAnswer);
                surveyAnswers.add(answer);
                permissionsService.addManagers(stakeholder.getManagers(), Collections.singletonList(answer.getId()));
                permissionsService.addContributors(stakeholder.getManagers(), Collections.singletonList(answer.getId()));
            }
        }
        return surveyAnswers;
    }
}
