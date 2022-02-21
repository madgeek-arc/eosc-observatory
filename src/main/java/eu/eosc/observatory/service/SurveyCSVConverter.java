package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.ChapterAnswer;
import eu.eosc.observatory.domain.HistoryEntry;
import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.SurveyAnswer;
import gr.athenarc.catalogue.ui.domain.Chapter;
import gr.athenarc.catalogue.ui.domain.Group;
import gr.athenarc.catalogue.ui.domain.Model;
import gr.athenarc.catalogue.ui.domain.UiField;
import gr.athenarc.catalogue.ui.service.ModelService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SurveyCSVConverter implements CSVConverter {

    private static final Logger logger = LogManager.getLogger(SurveyCSVConverter.class);

    private static final String DELIMITER_PRIMARY = "\t";
    private static final String DELIMITER_SECONDARY = ";";

    private final ModelService modelService;
    private final SurveyAnswerCrudService surveyAnswerCrudService;
    private final StakeholderService stakeholderService;
    private final UserService userService;

    @Autowired
    public SurveyCSVConverter(ModelService modelService,
                              SurveyAnswerCrudService surveyAnswerCrudService,
                              StakeholderService stakeholderService,
                              UserService userService) {
        this.modelService = modelService;
        this.surveyAnswerCrudService = surveyAnswerCrudService;
        this.stakeholderService = stakeholderService;
        this.userService = userService;
    }

    @Override
    public String convertToCSV(String modelId, boolean includeSensitiveData) {
        StringBuilder csv = new StringBuilder();
        Model model = modelService.get(modelId);
        Map<String, List<List<UiField>>> chapterFields = getChapterFields(model);
        Set<String> allKeys = new TreeSet<>();
        for (Map.Entry<String, List<List<UiField>>> entry : chapterFields.entrySet()) {
            allKeys.addAll(getKeys(entry.getValue()));
        }
//        Collections.sort(allKeys);
        csv.append(String.join(DELIMITER_PRIMARY, "Stakeholder Id", "Stakeholder Name"));
        csv.append(DELIMITER_PRIMARY);
        if (includeSensitiveData) {
            csv.append("Edited By");
            csv.append(DELIMITER_PRIMARY);
        }
        csv.append(String.join(DELIMITER_PRIMARY, allKeys));

        Set<SurveyAnswer> answerSet = surveyAnswerCrudService.getWithFilter("surveyId", model.getId());
        for (SurveyAnswer answer : answerSet) {
            Map<String, List<String>> results = toCsv(chapterFields, answer);
            List<String> row = new LinkedList<>();
            row.add(answer.getStakeholderId());
            Stakeholder stakeholder = stakeholderService.get(answer.getStakeholderId());
            row.add(stakeholder.getName());
            if (includeSensitiveData) {
                Set<String> contributorIds = answer.getHistory().getEntries().stream().map(HistoryEntry::getUserId).collect(Collectors.toSet());
                String contributors = contributorIds.stream().map(userService::get).map(user -> String.format("%s (%s)", user.getFullname(), user.getId())).collect(Collectors.joining(DELIMITER_SECONDARY));
                row.add(contributors);
            }
            for (String key : allKeys) {
                if (!results.containsKey(key) || results.get(key).isEmpty()) {
                    row.add("");
                } else {
                    StringBuilder entry = new StringBuilder();
                    for (Object val : results.get(key)) {
                        entry.append(val);
                        entry.append(DELIMITER_SECONDARY);
                    }
                    row.add(entry.substring(0, entry.toString().length() - 1)); // remove trailing ;
//                    row.add(String.join(DELIMITER_SECONDARY, results.get(key)));
                }
            }
            csv.append("\n");
            csv.append(String.join(DELIMITER_PRIMARY, row));
        }

        return csv.toString();
    }

    private Map<String, List<List<UiField>>> getChapterFields(Model model) {
        Map<String, List<List<UiField>>> chapterFields = new LinkedHashMap<>();
        for (Chapter chapter : model.getChapters()) {
            List<List<UiField>> fields = new LinkedList<>();
            for (Group section : chapter.getSections()) {
                for (UiField field : section.getFields()) {
                    fields.addAll(fieldsToLeaf(null, field));
                }
            }
            chapterFields.put(chapter.getId(), fields);
        }
        return chapterFields;
    }

    private List<List<UiField>> fieldsToLeaf(List<UiField> leafFieldList, UiField current) {
        List<List<UiField>> allLeafFieldLists = new LinkedList<>();
        if (leafFieldList == null) {
            leafFieldList = new LinkedList<>();
        }
        leafFieldList.add(current);
        if (current.getSubFields() != null && !current.getSubFields().isEmpty()) {
            for (UiField field : current.getSubFields()) {
                List<UiField> list = new LinkedList<>(leafFieldList);
//                list.add(field);
                allLeafFieldLists.addAll(fieldsToLeaf(list, field));
            }
        } else {
            if (!leafFieldList.isEmpty()) {
                allLeafFieldLists.add(leafFieldList);
            }
        }
        return allLeafFieldLists;
    }

    private Map<String, List<String>> toCsv(Map<String, List<List<UiField>>> chapterFields, SurveyAnswer surveyAnswer) {
        Map<String, List<String>> resultsMap = new LinkedHashMap<>();
        if (surveyAnswer.getChapterAnswers() != null && !surveyAnswer.getChapterAnswers().isEmpty()) {
            for (ChapterAnswer chapterAnswer : surveyAnswer.getChapterAnswers().values()) {
                List<List<UiField>> fields = chapterFields.get(chapterAnswer.getChapterId());
                JSONObject answer = chapterAnswer.getAnswer();
                for (List<UiField> fieldsToLeaf : fields) {
                    Pair<String, List<String>> keyValue = getValue(fieldsToLeaf, answer);
                    resultsMap.putIfAbsent(keyValue.getFirst(), new LinkedList<>());
                    resultsMap.get(keyValue.getFirst()).addAll(keyValue.getSecond());
                }
            }
        }
        return resultsMap;
    }

    private Set<String> getKeys(List<List<UiField>> fields) {
        Set<String> keys = new LinkedHashSet<>();
        for (List<UiField> fieldsToLeaf : fields) {
            keys.add(getKey(fieldsToLeaf));
        }
        return keys;
    }

    private String getKey(List<UiField> fieldsToLeaf) {
        String key = "";

        for (UiField field : fieldsToLeaf) {
            key = String.format("%s-%s", key, field.getLabel().getText());
        }

        if (key.startsWith("-")) {
            key = key.replaceFirst("-", "");
        }
        return key;
    }

    private Pair<String, List<String>> getValue(List<UiField> fieldsToLeaf, JSONObject answer) {
        List values = new LinkedList<>();
        Pair<String, List<String>> pair;
        String key = "";
        Object temp = null;
        if (answer != null) {
            temp = JSONValue.parse(answer.toJSONString());
        }
        for (UiField field : fieldsToLeaf) {
            key = String.format("%s-%s", key, field.getLabel().getText());
            if (temp instanceof JSONObject && ((JSONObject) temp).containsKey(field.getName())) {
                temp = ((JSONObject) temp).get(field.getName());
            } else {
                temp = null;
            }
        }

        if (temp != null) {
            if (fieldsToLeaf.get(fieldsToLeaf.size() - 1).getTypeInfo().isMultiplicity()) {
                values.addAll((JSONArray) temp);
            } else {
                values.add(temp);
            }
        }
        if (key.startsWith("-")) {
            key = key.replaceFirst("-", "");
        }
        pair = Pair.of(key, values);
        return pair;
    }
}
