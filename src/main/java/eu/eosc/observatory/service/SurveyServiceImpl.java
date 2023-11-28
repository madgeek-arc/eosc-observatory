package eu.eosc.observatory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.eosc.observatory.domain.*;
import eu.eosc.observatory.dto.*;
import eu.eosc.observatory.permissions.Groups;
import eu.eosc.observatory.permissions.PermissionService;
import eu.eosc.observatory.permissions.Permissions;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.service.GenericItemService;
import gr.athenarc.catalogue.ui.domain.Model;
import gr.athenarc.catalogue.ui.domain.Section;
import gr.athenarc.catalogue.ui.domain.UiField;
import gr.athenarc.catalogue.ui.service.ModelService;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SurveyServiceImpl implements SurveyService {

    private static final Logger logger = LoggerFactory.getLogger(SurveyServiceImpl.class);

    private final CrudService<Stakeholder> stakeholderCrudService;
    private final CrudService<SurveyAnswer> surveyAnswerCrudService;
    private final GenericItemService genericItemService;
    private final PermissionService permissionService;
    private final ModelService modelService;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SurveyServiceImpl(CrudService<Stakeholder> stakeholderCrudService,
                             CrudService<SurveyAnswer> surveyAnswerCrudService,
                             @Qualifier("catalogueGenericItemService") GenericItemService genericItemService,
                             PermissionService permissionService,
                             ModelService modelService,
                             UserService userService) {
        this.stakeholderCrudService = stakeholderCrudService;
        this.surveyAnswerCrudService = surveyAnswerCrudService;
        this.genericItemService = genericItemService;
        this.permissionService = permissionService;
        this.modelService = modelService;
        this.userService = userService;
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
        filter.addFilter("stakeholderId", id);
        return surveyAnswerCrudService.getAll(filter).getResults();
    }

    @Override
    public SurveyAnswer update(String id, SurveyAnswer surveyAnswer, String comment, Authentication authentication) throws ResourceNotFoundException {
        Date date = new Date();
        SurveyAnswer existing = surveyAnswerCrudService.get(id);
        User user = User.of(authentication);
        String userRole = getUserRole(authentication, existing.getStakeholderId());

        surveyAnswer.setHistory(existing.getHistory());
        surveyAnswer.getHistory().addEntry(user.getId(), userRole, comment, date, History.HistoryAction.UPDATED);
        surveyAnswer.getMetadata().setModifiedBy(user.getId());
        surveyAnswer.getMetadata().setModificationDate(date);
        return surveyAnswerCrudService.update(id, surveyAnswer);
    }

    @Override
    public SurveyAnswer importAnswer(String surveyAnswerId, String modelFrom, Authentication authentication) throws ResourceNotFoundException {
        Date date = new Date();
        Model modelToImport = modelService.get(modelFrom);
        SurveyAnswer surveyAnswer = surveyAnswerCrudService.get(surveyAnswerId);

        validateImportable(surveyAnswer.getSurveyId(), modelToImport);

        SurveyAnswer previous = getLatest(modelFrom, surveyAnswer.getStakeholderId());

        User user = User.of(authentication);
        String userRole = getUserRole(authentication, surveyAnswer.getStakeholderId());

        surveyAnswer.setAnswer(previous.getAnswer());

        surveyAnswer.getHistory().addEntry(user.getId(), userRole, String.format("Imported data from '%s'", modelToImport.getName()), date, History.HistoryAction.IMPORTED);
        surveyAnswer.getMetadata().setModifiedBy(user.getId());
        surveyAnswer.getMetadata().setModificationDate(date);
        return surveyAnswerCrudService.update(surveyAnswerId, surveyAnswer);
    }

    private void validateImportable(String surveyId, Model modelToImport) {
        Model model = modelService.get(surveyId);
        if (!model.getConfiguration().isPrefillable() || model.getConfiguration().getImportFrom() == null
                || model.getConfiguration().getImportFrom().isEmpty() || !model.getConfiguration().getImportFrom().contains(modelToImport.getId())) {
            throw new UnsupportedOperationException("Import from '" + modelToImport.getName() + "' is not supported.");
        }
    }

    @Override
    public SurveyAnswer updateAnswer(String surveyAnswerId, JSONObject answer, String comment, Authentication authentication) throws ResourceNotFoundException {
        Date date = new Date();
        SurveyAnswer surveyAnswer = surveyAnswerCrudService.get(surveyAnswerId);
        if (!hasChanged(surveyAnswer.getAnswer(), answer)) {
            return surveyAnswer;
        }
        User user = User.of(authentication);
        String userRole = getUserRole(authentication, surveyAnswer.getStakeholderId());

        surveyAnswer.setAnswer(answer);
        surveyAnswer.getHistory().addEntry(user.getId(), userRole, comment, date, History.HistoryAction.UPDATED);
        surveyAnswer.getMetadata().setModifiedBy(user.getId());
        surveyAnswer.getMetadata().setModificationDate(date);
        return surveyAnswerCrudService.update(surveyAnswerId, surveyAnswer);
    }

    @Override
    public Diff surveyAnswerDiff(String surveyAnswerId, String version1Id, String version2Id) {
        SurveyAnswer answer1 = surveyAnswerCrudService.getVersion(surveyAnswerId, version1Id);
        SurveyAnswer answer2 = surveyAnswerCrudService.getVersion(surveyAnswerId, version2Id);
        JsonNode node1 = null;
        JsonNode node2 = null;
        try {
            node1 = objectMapper.readTree(answer1.getAnswer().toJSONString());
            node2 = objectMapper.readTree(answer2.getAnswer().toJSONString());
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
        Diff diff = new Diff();
        if (node1 != null && node2 != null && !node1.equals(node2)) {
            diff.setDifferences(getDiff(node2, node1));
        }
        return diff;
    }

    List<Node> getDiff(JsonNode latestRoot, JsonNode previousRoot) {
        List<Node> differences = new LinkedList<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = latestRoot.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            if (previousRoot == null) {
                // skip children fields (show change only on top level field)
//                if (!entry.getValue().isNull()) {
//                    differences.add(new Node(entry.getKey(), new Modification(null, entry.getValue()), getDiff(entry.getValue(), null)));
//                }
            } else if (!entry.getValue().isNull() && previousRoot.get(entry.getKey()) != null && !previousRoot.get(entry.getKey()).isNull()) {
                if (entry.getValue().isMissingNode() != previousRoot.get(entry.getKey()).isMissingNode()
                        || (entry.getValue().isArray() && (!entry.getValue().equals(previousRoot.get(entry.getKey()))))) {
                    differences.add(new Node(entry.getKey(), new Modification(previousRoot.get(entry.getKey()), entry.getValue()), getDiff(entry.getValue(), previousRoot.get(entry.getKey()))));
                }
                List<Node> inside = getDiff(entry.getValue(), previousRoot.get(entry.getKey()));
                if (!inside.isEmpty()) {
                    differences.add(new Node(entry.getKey(), null, inside));
                }
            } else if (!entry.getValue().equals(previousRoot.get(entry.getKey()))) {
                differences.add(new Node(entry.getKey(), new Modification(previousRoot.get(entry.getKey()), entry.getValue()), getDiff(entry.getValue(), previousRoot.get(entry.getKey()))));
            }
        }
        return differences;
    }

    private boolean hasChanged(JSONObject before, JSONObject after) {
        JsonNode b;
        JsonNode a;
        try {
            b = objectMapper.readTree(before.toJSONString());
            a = objectMapper.readTree(after.toJSONString());
            return !b.equals(a);
        } catch (JsonProcessingException e) {
            logger.error("Error reading json.", e);
        }
        return true;
    }

    @Override
    public SurveyAnswer setAnswerValidated(String answerId, boolean validated, Authentication authentication) throws ResourceNotFoundException {
        Date date = new Date();
        SurveyAnswer surveyAnswer = surveyAnswerCrudService.get(answerId);
        User user = User.of(authentication);
        String userRole = getUserRole(authentication, surveyAnswer.getStakeholderId());
        if (surveyAnswer.isValidated() != validated) {
            History.HistoryAction action = validated ? History.HistoryAction.VALIDATED : History.HistoryAction.INVALIDATED;
            surveyAnswer.getHistory().addEntry(user.getId(), userRole, "", date, action);
            surveyAnswer.getMetadata().setModifiedBy(user.getId());
            surveyAnswer.getMetadata().setModificationDate(date);
            return validated ? validateAnswer(surveyAnswer) : invalidateAnswer(surveyAnswer);
        }
        return surveyAnswer;
    }

    @Override
    public SurveyAnswer setAnswerPublished(String answerId, boolean published, Authentication authentication) throws ResourceNotFoundException {
        throw new UnsupportedOperationException("Not implemented yet...");
        // TODO: implement this method
    }

    @Override
    public List<SurveyAnswer> generateAnswers(String surveyId, Authentication authentication) {
        Model survey = genericItemService.get("model", surveyId);
        logger.debug("Generating new cycle of Survey Answers for Survey: [id={}] [name={}]", survey.getId(), survey.getName());
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
        logger.info("Generating SurveyAnswer: [surveyId={}] [stakeholderId={}]", survey.getId(), stakeholder.getId());
        Metadata metadata = new Metadata(authentication);
        Date creationDate = metadata.getCreationDate();
        User user = User.of(authentication);
        String userRole = getUserRole(authentication, stakeholder.getId());

        // create answer for every stakeholder
        SurveyAnswer surveyAnswer = new SurveyAnswer();
        surveyAnswer.setMetadata(metadata);
        surveyAnswer.getHistory().addEntry(user.getId(), userRole, "", creationDate, History.HistoryAction.CREATED);

        surveyAnswer.setStakeholderId(stakeholder.getId());
        surveyAnswer.setSurveyId(survey.getId());
        surveyAnswer.setType(survey.getType());

        surveyAnswer = surveyAnswerCrudService.add(surveyAnswer);

        return surveyAnswer;
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
        for (SurveyAnswer answer : surveyAnswerBrowsing.getResults()) {
            logger.debug("SurveyAnswer [id={}]", answer.getId());
            Model survey = genericItemService.get("model", answer.getSurveyId());
            Stakeholder stakeholder = genericItemService.get("stakeholder", answer.getStakeholderId());
            SurveyAnswerInfo info = SurveyAnswerInfo.composeFrom(answer, survey, StakeholderInfo.of(stakeholder));
            setProgress(info, answer, survey);
            results.add(info);
        }
        surveyAnswerInfoBrowsing.setResults(results);
        return surveyAnswerInfoBrowsing;
    }

    private void setProgress(SurveyAnswerInfo info, SurveyAnswer surveyAnswer, Model survey) {
        Map<String, UiField> sectionFieldsMap;

        Progress required = new Progress();
        Progress total = new Progress();

        for (Section section : survey.getSections()) {
            Map<String, ?> answer = (Map) surveyAnswer.getAnswer();
            if (answer != null && !answer.isEmpty() && answer.get(section.getName()) != null) {
                answer = (Map) surveyAnswer.getAnswer().get(section.getName());
            }
            sectionFieldsMap = getFieldsMap(modelService.getSectionFields(section));
            for (UiField field : sectionFieldsMap.values()) {
                if ("question".equals(field.getKind())) {
                    total.addToTotal(1);
                    if (Boolean.TRUE.equals(field.getForm().getMandatory())) {
                        required.addToTotal(1);
                    }

                    if (fieldIsAnswered(field, answer, sectionFieldsMap)) {
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

    public Map<String, UiField> getFieldsMap(List<UiField> fields) {
        Map<String, UiField> allFieldsMap = new TreeMap<>();

        for (UiField field : fields) {
            allFieldsMap.put(field.getId(), field);
        }
        return allFieldsMap;
    }

    private boolean fieldIsAnswered(UiField field, Map<String, ?> chapterAnswer, Map<String, UiField> allFields) {
        if (chapterAnswer != null && !chapterAnswer.isEmpty()) {
            if (!"composite".equals(field.getTypeInfo().getType())) {
                return getValueFromAnswer(field, chapterAnswer, allFields) != null;
            }
            for (UiField f : field.getSubFields()) {
                if (getValueFromAnswer(f, chapterAnswer, allFields) != null)
                    return true;
            }
        }
        return false;
    }

    @Override
    public Object getValueFromAnswer(UiField field, Map<String, ?> answer, Map<String, UiField> allFields) {
        if (answer == null) {
            return null;
        }
        Deque<UiField> fields = new ArrayDeque<>();
        while (field != null) {
            fields.push(field);
            field = field.getParentId() != null ? allFields.get(field.getParentId()) : null;
        }

        Object object = answer;
        while (!fields.isEmpty()) {
            field = fields.pop();
            if (object instanceof Map) {
                if (((Map) object).get(field.getName()) != null) {
                    object = ((Map) object).get(field.getName());
                } else {
                    return null;
                }
            }
        }
        if ("".equals(object)) {
            object = null;
        } else if (object instanceof Map) {
            return checkIfContainsNonNull((Map) object) ? true : null;
        }
        return object;
    }

    boolean checkIfContainsNonNull(Map<String, ?> object) {
        boolean contains = false;
        for (Object obj : object.values()) {
            if (obj == null) {
                continue;
            } else if (obj instanceof Map) {
                contains = checkIfContainsNonNull((Map) obj);
                if (contains) {
                    break;
                }
            } else {
                return true;
            }
        }
        return contains;
    }

    @Override
    public void lockSurveyAndAnswers(String surveyId, boolean lock) {
        Model survey = modelService.get(surveyId);
        Set<Stakeholder> stakeholders = stakeholderCrudService.getWithFilter("type", survey.getType());
        Set<String> permissions = Collections.singleton(Permissions.WRITE.getKey());
        Set<String> managerPermissions = Set.of(Permissions.WRITE.getKey(), Permissions.MANAGE.getKey());
        for (Stakeholder stakeholder : stakeholders) {
            SurveyAnswer answer = getLatest(surveyId, stakeholder.getId());
            if (answer == null)
                continue;
            if (lock) {
                permissionService.removePermissions(stakeholder.getAdmins(), managerPermissions, Collections.singleton(answer.getId()));
                permissionService.removePermissions(stakeholder.getMembers(), permissions, Collections.singleton(answer.getId()));
            } else {
                permissionService.addPermissions(stakeholder.getAdmins(), managerPermissions, Collections.singleton(answer.getId()), Groups.STAKEHOLDER_MANAGER.getKey());
                permissionService.addPermissions(stakeholder.getMembers(), permissions, Collections.singleton(answer.getId()), Groups.STAKEHOLDER_CONTRIBUTOR.getKey());
            }
        }
        survey.setLocked(lock);
        modelService.update(surveyId, survey);
    }

    @Override
    public HistoryDTO getHistory(String surveyAnswerId) {
        HistoryDTO history = surveyAnswerCrudService.getHistory(surveyAnswerId, this::createHistoryEntry);
        return enrichHistory(history);
    }

    private HistoryDTO enrichHistory(HistoryDTO history) {
        List<HistoryEntryDTO> restored = history.getEntries()
                .stream()
                .filter(entry -> entry.getAction().getType().equals(History.HistoryAction.RESTORED))
                .collect(Collectors.toList());
        if (!restored.isEmpty()) {
            Map<String, HistoryEntryDTO> byVersion = history.getEntries().stream().collect(Collectors.toMap(HistoryEntryDTO::getVersion, Function.identity()));
            for (HistoryEntryDTO entry : restored) {
                entry.getAction().setPointsTo(byVersion.get(entry.getAction().getRegistryVersion()));
            }
        }
        return history;
    }

    private HistoryEntryDTO createHistoryEntry(Object object) {
        HistoryEntryDTO entry = new HistoryEntryDTO();
        if (object instanceof SurveyAnswer) {
            User user;
            String userId = ((SurveyAnswer) object).getMetadata().getModifiedBy();
            try {
                user = userService.get(userId);
            } catch (gr.athenarc.catalogue.exception.ResourceNotFoundException e) {
                user = new User();
                user.setId(userId);
            }
            List<HistoryEntry> historyEntryList = ((SurveyAnswer) object).getHistory().getEntries();
            entry = HistoryEntryDTO.of(historyEntryList.get(historyEntryList.size() - 1), user);
        }
        return entry;
    }

    @Override
    public SurveyAnswerMetadataDTO getPublicMetadata(String surveyAnswerId) {
        SurveyAnswerMetadataDTO metadata = new SurveyAnswerMetadataDTO();
        SurveyAnswer surveyAnswer = this.surveyAnswerCrudService.get(surveyAnswerId);
        metadata.setLastUpdate(surveyAnswer.getMetadata().getModificationDate());
        Map<String, User> editors = new TreeMap<>();
        for (HistoryEntry entry : surveyAnswer.getHistory().getEntries()) {
            if (!editors.containsKey(entry.getUserId()) &&
                    (entry.getAction() == History.HistoryAction.UPDATED
                            || entry.getAction() == History.HistoryAction.VALIDATED)) {
                User user = userService.get(entry.getUserId());
                editors.putIfAbsent(user.getId(), user);
            }
        }
        metadata.setEditors(editors.values().stream().toList());
        return metadata;
    }

    @Override
    public SurveyAnswer restore(String surveyAnswerId, String versionId) {
        return surveyAnswerCrudService.restore(surveyAnswerId, versionId, a -> createRestoreHistory(a, versionId));
    }

    private SurveyAnswer createRestoreHistory(SurveyAnswer answer, String version) {
        Date now = new Date();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = User.of(authentication);
        String userRole = getUserRole(authentication, answer.getStakeholderId());

        Metadata metadata = answer.getMetadata();
        metadata.setModifiedBy(user.getId());
        metadata.setModificationDate(now);
        answer.setMetadata(metadata);
        answer.getHistory().addEntry(user.getId(), userRole, "", now, History.HistoryAction.RESTORED, version);

        return answer;
    }

    private SurveyAnswer validateAnswer(SurveyAnswer surveyAnswer) throws ResourceNotFoundException {
        Stakeholder stakeholder = stakeholderCrudService.get(surveyAnswer.getStakeholderId());
        Set<String> members = stakeholder.getAdmins();
        members.addAll(stakeholder.getMembers());
        permissionService.removePermissions(members, Collections.singletonList(Permissions.WRITE.getKey()), Collections.singletonList(surveyAnswer.getId()));
        surveyAnswer.setValidated(true);
        return surveyAnswerCrudService.update(surveyAnswer.getId(), surveyAnswer);
    }

    private SurveyAnswer invalidateAnswer(SurveyAnswer surveyAnswer) throws ResourceNotFoundException {
        Stakeholder stakeholder = stakeholderCrudService.get(surveyAnswer.getStakeholderId());
        Set<String> members = stakeholder.getAdmins();
        permissionService.addPermissions(members, Collections.singletonList(Permissions.WRITE.getKey()), Collections.singletonList(surveyAnswer.getId()), Groups.STAKEHOLDER_MANAGER.getKey());
        members = stakeholder.getMembers();
        permissionService.addPermissions(members, Collections.singletonList(Permissions.WRITE.getKey()), Collections.singletonList(surveyAnswer.getId()), Groups.STAKEHOLDER_CONTRIBUTOR.getKey());
        surveyAnswer.setValidated(false);
        return surveyAnswerCrudService.update(surveyAnswer.getId(), surveyAnswer);
    }

    public String getUserRole(Authentication authentication, String stakeholderId) {
        UserInfo userInfo = userService.getUserInfo(authentication);

        for (GrantedAuthority grantedAuth : authentication.getAuthorities()) {
            if (grantedAuth.getAuthority().contains("ADMIN")) {
                return Roles.Administrative.ADMINISTRATOR.getRoleName();
            }
        }

        Stakeholder stakeholder = stakeholderCrudService.get(stakeholderId);
        if (userInfo.getCoordinators() != null && userInfo.getCoordinators().stream().anyMatch(coordinator -> coordinator.getType().equals(stakeholder.getType()))) {
            return Roles.Coordinator.COORDINATOR_MEMBER.getRoleName();
        }
        if (stakeholder.getAdmins() != null && stakeholder.getAdmins().contains(userInfo.getUser().getId())) {
            return Roles.Stakeholder.MANAGER.getRoleName();
        }
        if (stakeholder.getMembers() != null && stakeholder.getMembers().contains(userInfo.getUser().getId())) {
            return Roles.Stakeholder.CONTRIBUTOR.getRoleName();
        }
        return null;
    }
}
