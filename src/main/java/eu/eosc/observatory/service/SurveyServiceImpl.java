package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Metadata;
import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.domain.User;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
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
    public final PermissionService permissionService;

    @Autowired
    public SurveyServiceImpl(CrudItemService<Stakeholder> stakeholderCrudService,
                             CrudItemService<SurveyAnswer> surveyAnswerCrudService,
                             @Qualifier("catalogueGenericItemService") GenericItemService genericItemService,
                             PermissionService permissionService) {
        this.stakeholderCrudService = stakeholderCrudService;
        this.surveyAnswerCrudService = surveyAnswerCrudService;
        this.genericItemService = genericItemService;
        this.permissionService = permissionService;
    }

    @Override
    public Browsing<Survey> getByType(FacetFilter filter, String type) {
        filter.setResourceType("survey");
        if (type != null && !"".equals(type)) {
            filter.addFilter("type", type);
        }
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
    public List<SurveyAnswer> getActive(String stakeholderId) {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("stakeholderId", stakeholderId);
        filter.addFilter("validated", false);
        Map<String, Object> sortBy = new HashMap<>();
        Map<String, Object> orderType = new HashMap<>();
        orderType.put("order", "desc");
        sortBy.put("creationDate", orderType);
        filter.setOrderBy(sortBy);

        Browsing<SurveyAnswer> answersBrowsing = surveyAnswerCrudService.getAll(filter);
        return answersBrowsing.getResults();
    }

    @Override
    public SurveyAnswer updateAnswer(String id, JSONObject answer, User user) throws ResourceNotFoundException {
        SurveyAnswer surveyAnswer = surveyAnswerCrudService.get(id);
        surveyAnswer.getMetadata().setModifiedBy(user.getId());
        surveyAnswer.getMetadata().setModificationDate(new Date());
        surveyAnswer.setAnswer(answer);
        surveyAnswer = surveyAnswerCrudService.update(id, surveyAnswer);
        return surveyAnswer;
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
                permissionService.addManagers(stakeholder.getManagers(), Collections.singletonList(answer.getId()));
                permissionService.addContributors(stakeholder.getManagers(), Collections.singletonList(answer.getId()));
            }
        }
        return surveyAnswers;
    }
}
