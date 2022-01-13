package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Metadata;
import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.StakeholderInfo;
import eu.eosc.observatory.dto.SurveyAnswerInfo;
import eu.eosc.observatory.permissions.Groups;
import eu.eosc.observatory.permissions.PermissionService;
import eu.eosc.observatory.permissions.Permissions;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.service.GenericItemService;
import gr.athenarc.catalogue.ui.domain.Chapter;
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
    public Browsing<Survey> getByStakeholder(FacetFilter filter, String stakeholderId) {
        filter.setResourceType("survey");
        if (stakeholderId != null && !"".equals(stakeholderId)) {
            Stakeholder stakeholder = stakeholderCrudService.get(stakeholderId);
            filter.addFilter("type", stakeholder.getType());
            if (stakeholder.getSubType() != null && !"".equals(stakeholder.getSubType())) {
                filter.addFilter("chapterSubTypes", stakeholder.getSubType());
            }
        }
        Browsing<Survey> surveyBrowsing = this.genericItemService.getResults(filter);
        return surveyBrowsing;
    }

    @Override
    public List<SurveyAnswer> getLatest(String surveyId, String stakeholderId) {
        Survey survey = genericItemService.get("survey", surveyId);
        FacetFilter filter = new FacetFilter();
        filter.addFilter("surveyId", surveyId);
        filter.addFilter("stakeholderId", stakeholderId);
        Map<String, Object> sortBy = new HashMap<>();
        Map<String, Object> orderType = new HashMap<>();
        orderType.put("order", "desc");
        sortBy.put("creationDate", orderType);
        filter.setOrderBy(sortBy);

        Browsing<SurveyAnswer> answersBrowsing = surveyAnswerCrudService.getAll(filter);
        List<SurveyAnswer> answers = new ArrayList<>();
        if (answersBrowsing.getTotal() >= survey.getChapters().size()) {
            for (int i = 0; i < survey.getChapters().size(); i++) {
                answers.add(answersBrowsing.getResults().get(i));
            }
        }
        return answers;
    }

    @Override
    public List<SurveyAnswer> getActive(String stakeholderId) {
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
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
    public List<SurveyAnswer> getAllByStakeholder(String id) {
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        return surveyAnswerCrudService.getAll(filter).getResults();
    }

    @Override
    public SurveyAnswer update(String id, SurveyAnswer surveyAnswer, User user) throws ResourceNotFoundException {
        surveyAnswer.getMetadata().setModifiedBy(user.getId());
        surveyAnswer.getMetadata().setModificationDate(new Date());
        return surveyAnswerCrudService.update(id, surveyAnswer);
    }

    @Override
    public SurveyAnswer updateAnswer(String id, JSONObject answer, User user) throws ResourceNotFoundException {
        SurveyAnswer surveyAnswer = surveyAnswerCrudService.get(id);
        surveyAnswer.setAnswer(answer);
        surveyAnswer = this.update(id, surveyAnswer, user);
        return surveyAnswer;
    }

    @Override
    public SurveyAnswer setAnswerValidated(String answerId, boolean validated, User user) throws ResourceNotFoundException {
        SurveyAnswer surveyAnswer = surveyAnswerCrudService.get(answerId);
        if (surveyAnswer.isValidated() != validated) {
            return validated ? validateAnswer(surveyAnswer, user) : invalidateAnswer(surveyAnswer, user);
        }
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
                for (Chapter chapter : survey.getChapters()) {
                    surveyAnswer.setChapterId(chapter.getId()); // create answer for every chapter
                    SurveyAnswer answer = surveyAnswerCrudService.add(surveyAnswer);
                    surveyAnswers.add(answer);
                    permissionService.addManagers(stakeholder.getManagers(), Collections.singletonList(answer.getId()));
                    permissionService.addContributors(stakeholder.getManagers(), Collections.singletonList(answer.getId()));
                }
            }
        }
        return surveyAnswers;
    }

    @Override
    public List<SurveyAnswer> generateAnswers(String surveyId, Authentication authentication) {
        Survey survey = genericItemService.get("survey", surveyId);
        logger.debug(String.format("Generating new cycle of Survey Answers for Survey: [id=%s] [name=%s]", survey.getId(), survey.getName()));
        List<SurveyAnswer> surveyAnswers = new ArrayList<>();
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        filter.addFilter("type", survey.getType());
        List<Stakeholder> stakeholders = this.stakeholderCrudService.getAll(filter).getResults();

        SurveyAnswer surveyAnswer = new SurveyAnswer();
        JSONObject object = new JSONObject();
        surveyAnswer.setAnswer(object);

        Metadata metadata = new Metadata(authentication);
        surveyAnswer.setMetadata(metadata);

        for (Chapter chapter : survey.getChapters()) {
            // create answer for every chapter
            surveyAnswer.setChapterId(chapter.getId());

            for (Stakeholder stakeholder : stakeholders) {
                // create answer for every stakeholder
                surveyAnswer.setStakeholderId(stakeholder.getId());
                surveyAnswer.setSurveyId(survey.getId());
                SurveyAnswer answer = surveyAnswerCrudService.add(surveyAnswer);
                surveyAnswers.add(answer);
                permissionService.addManagers(stakeholder.getManagers(), Collections.singletonList(answer.getId()));
                permissionService.addContributors(stakeholder.getManagers(), Collections.singletonList(answer.getId()));
            }
        }
        return surveyAnswers;
    }

    @Override // TODO: this is just a draft
    public Browsing<SurveyAnswerInfo> browseSurveyAnswersInfo(FacetFilter filter) {
        filter.setResourceType("survey_answer");
        Browsing<SurveyAnswer> surveyAnswerBrowsing = genericItemService.getResults(filter);
        Browsing<SurveyAnswerInfo> surveyAnswerInfoBrowsing = new Browsing<>();
        surveyAnswerInfoBrowsing.setFrom(surveyAnswerBrowsing.getFrom());
        surveyAnswerInfoBrowsing.setTotal(surveyAnswerBrowsing.getTotal());
        surveyAnswerInfoBrowsing.setFacets(surveyAnswerBrowsing.getFacets());
        List<SurveyAnswerInfo> results = new ArrayList<>();
        for (SurveyAnswer answer : surveyAnswerBrowsing.getResults()) {
            Survey survey = genericItemService.get("survey", answer.getSurveyId());
            Stakeholder stakeholder = genericItemService.get("stakeholder", answer.getStakeholderId());
            results.add(SurveyAnswerInfo.composeFrom(answer, survey, StakeholderInfo.of(stakeholder)));
        }
        surveyAnswerInfoBrowsing.setResults(results);
        return surveyAnswerInfoBrowsing;
    }


    private SurveyAnswer validateAnswer(SurveyAnswer surveyAnswer, User user) throws ResourceNotFoundException {
        Stakeholder stakeholder = stakeholderCrudService.get(surveyAnswer.getStakeholderId());
        List<String> members = stakeholder.getManagers();
        members.addAll(stakeholder.getContributors());
        permissionService.removePermissions(members, Collections.singletonList(Permissions.WRITE.getKey()), Collections.singletonList(surveyAnswer.getId()));
        surveyAnswer.setValidated(true);
        return this.update(surveyAnswer.getId(), surveyAnswer, user);
    }

    private SurveyAnswer invalidateAnswer(SurveyAnswer surveyAnswer, User user) throws ResourceNotFoundException {
        Stakeholder stakeholder = stakeholderCrudService.get(surveyAnswer.getStakeholderId());
        List<String> members = stakeholder.getManagers();
        permissionService.addPermissions(members, Collections.singletonList(Permissions.WRITE.getKey()), Collections.singletonList(surveyAnswer.getId()), Groups.STAKEHOLDER_MANAGER.getKey());
        members = stakeholder.getContributors();
        permissionService.addPermissions(members, Collections.singletonList(Permissions.WRITE.getKey()), Collections.singletonList(surveyAnswer.getId()), Groups.STAKEHOLDER_CONTRIBUTOR.getKey());
        surveyAnswer.setValidated(false);
        return this.update(surveyAnswer.getId(), surveyAnswer, user);
    }
}
