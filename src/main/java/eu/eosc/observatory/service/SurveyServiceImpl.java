package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.*;
import eu.eosc.observatory.dto.Progress;
import eu.eosc.observatory.dto.StakeholderInfo;
import eu.eosc.observatory.dto.SurveyAnswerInfo;
import eu.eosc.observatory.permissions.Groups;
import eu.eosc.observatory.permissions.PermissionService;
import eu.eosc.observatory.permissions.Permissions;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.service.GenericItemService;
import gr.athenarc.catalogue.service.id.IdGenerator;
import gr.athenarc.catalogue.ui.domain.*;
import gr.athenarc.catalogue.ui.service.FormsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SurveyServiceImpl implements SurveyService {

    private static final Logger logger = LoggerFactory.getLogger(SurveyServiceImpl.class);

    private final CrudItemService<Stakeholder> stakeholderCrudService;
    private final CrudItemService<SurveyAnswer> surveyAnswerCrudService;
    private final GenericItemService genericItemService;
    private final PermissionService permissionService;
    private final IdGenerator<String> idGenerator;
    private final FormsService formsService;

    @Autowired
    public SurveyServiceImpl(CrudItemService<Stakeholder> stakeholderCrudService,
                             CrudItemService<SurveyAnswer> surveyAnswerCrudService,
                             @Qualifier("catalogueGenericItemService") GenericItemService genericItemService,
                             PermissionService permissionService,
                             IdGenerator<String> idGenerator,
                             FormsService formsService) {
        this.stakeholderCrudService = stakeholderCrudService;
        this.surveyAnswerCrudService = surveyAnswerCrudService;
        this.genericItemService = genericItemService;
        this.permissionService = permissionService;
        this.idGenerator = idGenerator;
        this.formsService = formsService;
    }

    @Override
    public Browsing<Model> getByType(FacetFilter filter, String type) {
        filter.setResourceType("model");
        if (type != null && !"".equals(type)) {
            filter.addFilter("type", type);
        }
        Browsing<Model> surveyBrowsing = this.genericItemService.getResults(filter);
        return surveyBrowsing;
    }

    @Override
    public Browsing<Model> getByStakeholder(FacetFilter filter, String stakeholderId) {
        filter.setResourceType("model");
        if (stakeholderId != null && !"".equals(stakeholderId)) {
            Stakeholder stakeholder = stakeholderCrudService.get(stakeholderId);
            filter.addFilter("type", stakeholder.getType());
            // TODO: need to implement OR filter
//            if (stakeholder.getSubType() != null && !"".equals(stakeholder.getSubType())) {
//                filter.setKeyword("chapterSubTypes=" + stakeholder.getSubType());
//            }
        }
        Browsing<Model> surveyBrowsing = this.genericItemService.getResults(filter);
        return surveyBrowsing;
    }

    @Override
    public SurveyAnswer getLatest(String surveyId, String stakeholderId) {
//        Model survey = genericItemService.get("model", surveyId);
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
        if (!answersBrowsing.getResults().isEmpty()) {
            answer = answersBrowsing.getResults().get(0);
        }
        return answer;
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
        Date date = new Date();
        SurveyAnswer existing = surveyAnswerCrudService.get(id);
        surveyAnswer.setHistory(existing.getHistory());
        surveyAnswer.getHistory().addEntry(user.getId(), date, null, History.HistoryAction.UPDATED);
        surveyAnswer.getMetadata().setModifiedBy(user.getId());
        surveyAnswer.getMetadata().setModificationDate(date);
        return surveyAnswerCrudService.update(id, surveyAnswer);
    }

    @Override
    public SurveyAnswer updateAnswer(String surveyAnswerId, String chapterAnswerId, JSONObject answer, User user) throws ResourceNotFoundException {
        Date date = new Date();
        SurveyAnswer surveyAnswer = surveyAnswerCrudService.get(surveyAnswerId);
        surveyAnswer.getChapterAnswers().get(chapterAnswerId).setAnswer(answer);
        surveyAnswer.getHistory().addEntry(user.getId(), date, surveyAnswer.getChapterAnswers().get(chapterAnswerId).getChapterId(), History.HistoryAction.UPDATED);
        surveyAnswer.getMetadata().setModifiedBy(user.getId());
        surveyAnswer.getMetadata().setModificationDate(date);
        surveyAnswer.getChapterAnswers().get(chapterAnswerId).setMetadata(surveyAnswer.getMetadata());
        return surveyAnswerCrudService.update(surveyAnswerId, surveyAnswer);
    }

    @Override
    public SurveyAnswer setAnswerValidated(String answerId, boolean validated, User user) throws ResourceNotFoundException {
        Date date = new Date();
        SurveyAnswer surveyAnswer = surveyAnswerCrudService.get(answerId);
        if (surveyAnswer.isValidated() != validated) {
            History.HistoryAction action = validated ? History.HistoryAction.VALIDATED : History.HistoryAction.INVALIDATED;
            surveyAnswer.getHistory().addEntry(user.getId(), date, null, action);
            surveyAnswer.getMetadata().setModifiedBy(user.getId());
            surveyAnswer.getMetadata().setModificationDate(date);
            return validated ? validateAnswer(surveyAnswer) : invalidateAnswer(surveyAnswer);
        }
        return surveyAnswer;
    }

    @Override
    public SurveyAnswer setAnswerPublished(String answerId, boolean published, User user) throws ResourceNotFoundException {
        throw new UnsupportedOperationException("Not implemented yet...");
        // TODO: implement this method
    }

    @Override
    public List<SurveyAnswer> generateAnswers(String surveyId, Authentication authentication) {
        Model survey = genericItemService.get("model", surveyId);
        logger.debug(String.format("Generating new cycle of Survey Answers for Survey: [id=%s] [name=%s]", survey.getId(), survey.getName()));
        List<SurveyAnswer> surveyAnswers = new ArrayList<>();
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        filter.addFilter("type", survey.getType());
        List<Stakeholder> stakeholders = this.stakeholderCrudService.getAll(filter).getResults();

        for (Stakeholder stakeholder : stakeholders) {
            // create answer for every stakeholder
            SurveyAnswer answer = generateAnswer(stakeholder, survey, authentication);
            surveyAnswers.add(answer);
        }
        return surveyAnswers;
    }

    @Override
    public SurveyAnswer generateStakeholderAnswer(String stakeholderId, String surveyId, Authentication authentication) {
        Stakeholder stakeholder = stakeholderCrudService.get(stakeholderId);
        Model survey = genericItemService.get("model", surveyId);
        return generateAnswer(stakeholder, survey, authentication);
    }

    private SurveyAnswer generateAnswer(Stakeholder stakeholder, Model survey, Authentication authentication) {
        logger.info(String.format("Generating SurveyAnswer: [surveyId=%s] [stakeholderId=%s]", survey.getId(), stakeholder.getId()));
        Metadata metadata = new Metadata(authentication);
        Date creationDate = metadata.getCreationDate();
        // create answer for every stakeholder
        SurveyAnswer surveyAnswer = new SurveyAnswer();
        surveyAnswer.setMetadata(metadata);
        surveyAnswer.getHistory().addEntry(User.of(authentication).getId(), creationDate, null, History.HistoryAction.CREATED);

        surveyAnswer.setStakeholderId(stakeholder.getId());
        surveyAnswer.setSurveyId(survey.getId());
        surveyAnswer.setType(survey.getType());

        // TODO: move this inside surveyAnswer constructor?
        for (Chapter chapter : survey.getChapters()) {
            // create answer for every chapter
            if (chapter.getSubType() != null && !Objects.equals(chapter.getSubType(), stakeholder.getSubType())) {
                // skip chapters not matching subtype
                continue;
            }
            String chapterAnswerId = generateChapterAnswerId();
            surveyAnswer.getHistory().addEntry(User.of(authentication).getId(), creationDate, chapter.getId(), History.HistoryAction.CREATED);
            ChapterAnswer chapterAnswer = new ChapterAnswer(chapterAnswerId, chapter.getId(), metadata);
            chapterAnswer.setId(chapterAnswerId);
            chapterAnswer.setChapterId(chapter.getId());

            surveyAnswer.getChapterAnswers().put(chapterAnswer.getId(), chapterAnswer);
        }

        surveyAnswer = surveyAnswerCrudService.add(surveyAnswer);

        return surveyAnswer;
    }

    @Override // TODO: optimize
    public Browsing<SurveyAnswerInfo> browseSurveyAnswersInfo(String type, FacetFilter filter) {
        filter.setResourceType("survey_answer");
        filter.addFilter("type", type);
        Browsing<SurveyAnswer> surveyAnswerBrowsing = genericItemService.getResults(filter);
        Browsing<SurveyAnswerInfo> surveyAnswerInfoBrowsing = new Browsing<>();
        surveyAnswerInfoBrowsing.setFrom(surveyAnswerBrowsing.getFrom());
        surveyAnswerInfoBrowsing.setTo(surveyAnswerBrowsing.getTo());
        surveyAnswerInfoBrowsing.setTotal(surveyAnswerBrowsing.getTotal());
        surveyAnswerInfoBrowsing.setFacets(surveyAnswerBrowsing.getFacets());
        List<SurveyAnswerInfo> results = new ArrayList<>();
        Map<String, Map<String, List<UiField>>> surveyChapterFields = new HashMap<>();
        for (SurveyAnswer answer : surveyAnswerBrowsing.getResults()) {
            logger.debug(String.format("SurveyAnswer [id=%s]", answer.getId()));
            Model survey = genericItemService.get("model", answer.getSurveyId());
            if (!surveyChapterFields.containsKey(survey.getId())) {
                surveyChapterFields.put(survey.getId(), formsService.getChapterFieldsMap(survey.getId()));
            }
            Stakeholder stakeholder = genericItemService.get("stakeholder", answer.getStakeholderId());
            SurveyAnswerInfo info = SurveyAnswerInfo.composeFrom(answer, survey, StakeholderInfo.of(stakeholder));
            setProgress(info, answer, surveyChapterFields.get(survey.getId()));
            results.add(info);
        }
        surveyAnswerInfoBrowsing.setResults(results);
        return surveyAnswerInfoBrowsing;
    }

    @Override // TODO: optimize
    public Browsing<SurveyAnswerInfo> browseSurveyAnswersInfo(FacetFilter filter) {
        filter.setResourceType("survey_answer");
        Browsing<SurveyAnswer> surveyAnswerBrowsing = genericItemService.getResults(filter);
        Browsing<SurveyAnswerInfo> surveyAnswerInfoBrowsing = new Browsing<>();
        surveyAnswerInfoBrowsing.setFrom(surveyAnswerBrowsing.getFrom());
        surveyAnswerInfoBrowsing.setTo(surveyAnswerBrowsing.getTo());
        surveyAnswerInfoBrowsing.setTotal(surveyAnswerBrowsing.getTotal());
        surveyAnswerInfoBrowsing.setFacets(surveyAnswerBrowsing.getFacets());
        List<SurveyAnswerInfo> results = new ArrayList<>();
        Map<String, Map<String, List<UiField>>> surveyChapterFields = new HashMap<>();
        for (SurveyAnswer answer : surveyAnswerBrowsing.getResults()) {
            logger.debug(String.format("SurveyAnswer [id=%s]", answer.getId()));
            Model survey = genericItemService.get("model", answer.getSurveyId());
            if (!surveyChapterFields.containsKey(survey.getId())) {
                surveyChapterFields.put(survey.getId(), formsService.getChapterFieldsMap(survey.getId()));
            }
            Stakeholder stakeholder = genericItemService.get("stakeholder", answer.getStakeholderId());
            SurveyAnswerInfo info = SurveyAnswerInfo.composeFrom(answer, survey, StakeholderInfo.of(stakeholder));
            setProgress(info, answer, surveyChapterFields.get(survey.getId()));
            results.add(info);
        }
        surveyAnswerInfoBrowsing.setResults(results);
        return surveyAnswerInfoBrowsing;
    }

    private List<UiField> getFieldsRecursive(List<UiField> fields) {
        List<UiField> allFields = new ArrayList<>();
        for (UiField field : fields) {
            allFields.add(field);
            if (field.getSubFields() != null) {
                allFields.addAll(getFieldsRecursive(field.getSubFields()));
            }
        }
        return allFields;
    }

    private Map<String, UiField> getAllFields(String surveyId) {
        Map<String, UiField> allFieldsMap = new TreeMap<>();
        Model survey = formsService.get(surveyId);
        List<UiField> allFields = survey.getChapters()
                .stream()
                .map(chapter -> chapter.getSections()
                        .stream()
                        .map(Group::getFields)
                        .collect(Collectors.toList())
                        .stream()
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        allFields = getFieldsRecursive(allFields);

        for (UiField field : allFields) {
            allFieldsMap.put(field.getId(), field);
        }
        return allFieldsMap;
    }

    // TODO: optimize
    private void setProgress(SurveyAnswerInfo info, SurveyAnswer answer, Map<String, List<UiField>> chapterFieldsMap) {
        Map<String, UiField> allFieldsMap = getAllFields(answer.getSurveyId());

        Progress required = new Progress();
        Progress total = new Progress();
        Map<String, JSONObject> chapterAnswers = new HashMap<>();
        for (ChapterAnswer chapterAnswer : answer.getChapterAnswers().values()) {
            chapterAnswers.put(chapterAnswer.getChapterId(), chapterAnswer.getAnswer());
        }
        for (Map.Entry<String, List<UiField>> chapter : chapterFieldsMap.entrySet()) {
            JSONObject chapterAnswer = chapterAnswers.get(chapter.getKey());
            for (UiField field : getFieldsRecursive(chapter.getValue())) {
                if ("question".equals(field.getKind())) {
                    total.addToTotal(1);
                    if (Boolean.TRUE.equals(field.getForm().getMandatory())) {
                        required.addToTotal(1);
                    }

                    if (fieldIsAnswered(field, chapterAnswer, allFieldsMap)) {
                        total.addToCurrent(1);
                        if (Boolean.TRUE.equals(field.getForm().getMandatory())) {
                            required.addToCurrent(1);
                        }
                    }
                }
            }
        }
        info.setProgressRequired(required);
        info.setProgressTotal(total);

    }

    private boolean fieldIsAnswered(UiField field, JSONObject chapterAnswer, Map<String, UiField> allFields) {
        if (!"composite".equals(field.getTypeInfo().getType())) {
            return getValueFromAnswer(field, chapterAnswer, allFields) != null;
        }
        for (UiField f : field.getSubFields()) {
            if (getValueFromAnswer(f, chapterAnswer, allFields) != null)
                return true;
        }
        return false;
    }

    @Override
    public Object getValueFromAnswer(UiField field, JSONObject answer, Map<String, UiField> allFields) {
        if (answer == null) {
            return null;
        }
        Deque<UiField> fields = new ArrayDeque<>();
        while (field != null && field.getParentId() != null) {
            fields.push(field);
//            field = formsService.getField(field.getParentId());
            field = allFields.get(field.getParentId()); // FIXME: REPLACE
        }

        Object object = JSONValue.parse(answer.toJSONString());
        while (!fields.isEmpty()) {
            field = fields.pop();
            if (object instanceof JSONObject) {
                if (((JSONObject) object).get(field.getName()) != null) {
                    object = ((JSONObject) object).get(field.getName());
                } else {
                    return null;
                }
            }
        }
        if ("".equals(object)) {
            object = null;
        }
        return object;
    }

    @Override
    public String generateChapterAnswerId() {
        return idGenerator.createId("ca-", 8);
    }


    private SurveyAnswer validateAnswer(SurveyAnswer surveyAnswer) throws ResourceNotFoundException {
        Stakeholder stakeholder = stakeholderCrudService.get(surveyAnswer.getStakeholderId());
        List<String> members = stakeholder.getManagers();
        members.addAll(stakeholder.getContributors());
        permissionService.removePermissions(members, Collections.singletonList(Permissions.WRITE.getKey()), Collections.singletonList(surveyAnswer.getId()));
        surveyAnswer.setValidated(true);
        return surveyAnswerCrudService.update(surveyAnswer.getId(), surveyAnswer);
    }

    private SurveyAnswer invalidateAnswer(SurveyAnswer surveyAnswer) throws ResourceNotFoundException {
        Stakeholder stakeholder = stakeholderCrudService.get(surveyAnswer.getStakeholderId());
        List<String> members = stakeholder.getManagers();
        permissionService.addPermissions(members, Collections.singletonList(Permissions.WRITE.getKey()), Collections.singletonList(surveyAnswer.getId()), Groups.STAKEHOLDER_MANAGER.getKey());
        members = stakeholder.getContributors();
        permissionService.addPermissions(members, Collections.singletonList(Permissions.WRITE.getKey()), Collections.singletonList(surveyAnswer.getId()), Groups.STAKEHOLDER_CONTRIBUTOR.getKey());
        surveyAnswer.setValidated(false);
        return surveyAnswerCrudService.update(surveyAnswer.getId(), surveyAnswer);
    }
}
