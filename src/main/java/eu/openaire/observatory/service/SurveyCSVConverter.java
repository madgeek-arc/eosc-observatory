/*
 * Copyright 2021-2025 OpenAIRE AMKE
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.openaire.observatory.service;

import eu.openaire.observatory.CsvBuilder;
import eu.openaire.observatory.domain.*;
import gr.uoa.di.madgik.catalogue.ui.domain.Model;
import gr.uoa.di.madgik.catalogue.ui.domain.Section;
import gr.uoa.di.madgik.catalogue.ui.domain.StyledString;
import gr.uoa.di.madgik.catalogue.ui.domain.UiField;
import gr.uoa.di.madgik.catalogue.service.ModelService;
import jakarta.validation.constraints.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SurveyCSVConverter implements CSVConverter {

    private static final Logger logger = LoggerFactory.getLogger(SurveyCSVConverter.class);

    private static final String DELIMITER_PRIMARY = "\t";
    private static final String DELIMITER_SECONDARY = ";";
    private static final String JOINING_SYMBOL = "->";
    private static final String DOUBLE_QUOTE_ENCODED = "%22";

    private final ModelService modelService;
    private final SurveyService surveyService;
    private final SurveyAnswerCrudService surveyAnswerCrudService;
    private final StakeholderService stakeholderService;
    private final UserService userService;

    private CsvBuilder csvBuilder;

    public SurveyCSVConverter(ModelService modelService,
                              SurveyService surveyService,
                              SurveyAnswerCrudService surveyAnswerCrudService,
                              StakeholderService stakeholderService,
                              UserService userService) {
        this.modelService = modelService;
        this.surveyService = surveyService;
        this.surveyAnswerCrudService = surveyAnswerCrudService;
        this.stakeholderService = stakeholderService;
        this.userService = userService;
    }

    private String[][] csvToTable(String data) {
        String[] rows = data.split("\n");
        String[][] table = new String[rows.length][];

        for (int i = 0; i < rows.length; i++) {
            table[i] = rows[i].split(DELIMITER_PRIMARY);
        }
        return table;
    }

    @Override
    public List<?> ingestFromCSV(String modelId, String data) {
        Model model = modelService.get(modelId);

        String[][] csvData = csvToTable(data);
        List<SurveyAnswer> surveyAnswers = new ArrayList<>();

        String[] headers = csvData[0];
        for (int i = 1; i < csvData.length; i++) { // for each row (except first)
            Model survey = modelService.get(modelId);
            SurveyAnswer surveyAnswer = new SurveyAnswer();
            surveyAnswer.setSurveyId(modelId);
            surveyAnswer.setType(survey.getType());
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Metadata metadata = new Metadata(authentication);
            Date creationDate = metadata.getCreationDate();
            surveyAnswer.setMetadata(metadata);
            surveyAnswer.getHistory().addEntry(User.getId(authentication), Roles.Administrative.ADMINISTRATOR.getRoleName(), "", creationDate, History.HistoryAction.CREATED);

            Map<String, List<UiField>> keyToFields = new TreeMap<>();
            for (List<UiField> fieldsToLeaf : getSectionFieldsList(model.getSections(), null)) {
                keyToFields.put(getKey(fieldsToLeaf), fieldsToLeaf);
            }
            surveyAnswer.getHistory().addEntry(User.getId(authentication), Roles.Administrative.ADMINISTRATOR.getRoleName(), "", creationDate, History.HistoryAction.CREATED);

            for (int j = 0; j < csvData[i].length; j++) { // for each column (csv headers)
                if (csvData[i][j] != null && !csvData[i][j].equals("") && keyToFields.containsKey(headers[j])) {
                    List<UiField> fieldList = keyToFields.get(headers[j]);
                    JSONObject subAnswer = surveyAnswer.getAnswer();
                    if (fieldList != null) {
                        for (int f = 0; f < fieldList.size() - 1; f++) {
                            if (subAnswer.containsKey(fieldList.get(f).getName())) {
                                subAnswer = (JSONObject) subAnswer.get(fieldList.get(f).getName());
                            } else {
                                JSONObject node = new JSONObject();
                                subAnswer.put(fieldList.get(f).getName(), node);
                                subAnswer = (JSONObject) subAnswer.get(fieldList.get(f).getName());
                            }
                        }
                        subAnswer.put(fieldList.get(fieldList.size() - 1).getName(), csvData[i][j]);
                    } else {
                        subAnswer.put(csvData[i][j], csvData[i][j]);
                    }
                } else {
                    switch (headers[j]) {
                        case "Stakeholder Id":
                            surveyAnswer.setStakeholderId(csvData[i][j]);
                            break;
                        default:
                            break;
                    }
                }
            }
            surveyAnswers.add(surveyAnswer);
            logger.info("Importing survey answer: {}", surveyAnswer);

            // TODO: create metadata and history entries
            surveyAnswer.setMetadata(new Metadata());

        }
        return surveyAnswers;
    }

    @Override
    public String convertToCSV(String modelId, boolean includeSensitiveData, Date from, Date to) {
        Model model = modelService.get(modelId);
        List<List<UiField>> fieldHierarchy = getFieldHierarchy(model);

        List<SurveyAnswer> answerSet = getSurveyAnswers(model, from, to);
        int index = 0;
        List<String> creationDates = answerSet
                .stream()
                .map(a -> a.getMetadata().getCreationDate().toString())
                .toList();
        List<String> stakeholderIds = answerSet
                .stream()
                .map(SurveyAnswer::getStakeholderId)
                .toList();
        List<String> stakeholderNames = answerSet
                .stream()
                .map(a -> stakeholderService.get(a.getStakeholderId()).getName())
                .toList();

        this.csvBuilder = new CsvBuilder();
        this.csvBuilder.addColumn("Creation Date", creationDates);
        this.csvBuilder.addColumn("Stakeholder Id", stakeholderIds);
        this.csvBuilder.addColumn("Stakeholder Names", stakeholderNames);

        if (includeSensitiveData) {
            List<String> contributorInfo = answerSet.stream().map(this::getContributorsInfo).toList();
            this.csvBuilder.addColumn("Edited By", contributorInfo);
        }

        for (SurveyAnswer surveyAnswer : answerSet) {
            JSONObject answer = surveyAnswer.getAnswer();
            for (List<UiField> fieldsToLeaf : fieldHierarchy) {
                createValues(null, fieldsToLeaf, answer, index);
            }
            index++;
        }

        return this.csvBuilder.toCsv();
    }

    private List<SurveyAnswer> getSurveyAnswers(Model model, Date from, Date to) {
        Set<SurveyAnswer> answerSet = new HashSet<>();
        if (from == null || to == null) {
            Set<Stakeholder> stakeholders = stakeholderService.getWithFilter("type", model.getType());
            for (Stakeholder sh : stakeholders) {
                answerSet.add(surveyService.getLatest(model.getId(), sh.getId()));
            }
        } else {
            for (SurveyAnswer answer : surveyAnswerCrudService.getWithFilter("surveyId", model.getId())) {
                if (answer.getMetadata().getCreationDate().after(to)
                        || answer.getMetadata().getCreationDate().before(from)) {
                    continue;
                }
                answerSet.add(answer);
            }
        }
        Comparator<SurveyAnswer> comparator = Comparator.comparing(a ->
        {
            Date date;
            if (a.getMetadata() != null && a.getMetadata().getCreationDate() != null) {
                date = a.getMetadata().getCreationDate();
            } else if (a.getMetadata() != null && a.getMetadata().getModificationDate() != null) {
                date = a.getMetadata().getModificationDate();
            } else {
                date = new Date();
            }
            return date;
        });

        return answerSet.stream().filter(Objects::nonNull).sorted(comparator).toList();
    }

    private String getContributorsInfo(SurveyAnswer answer) {
        Set<String> contributorIds = answer.getHistory().getEntries()
                .stream()
                .map(history ->
                        !history.getAction().equals(History.HistoryAction.CREATED) ? history.getEditors() : null)
                .filter(Objects::nonNull)
                .flatMap(editor -> editor.stream().map(Editor::getUser))
                .collect(Collectors.toSet());
        return contributorIds
                .stream()
                .map(userService::get)
                .map(user -> String.format("%s (%s)", user.getFullname(), user.getId()))
                .collect(Collectors.joining(DELIMITER_SECONDARY));
    }

    private List<UiField> sortFieldList(@NotNull List<UiField> fieldList) {
        Comparator<UiField> orderComparator = Comparator.comparing(f -> f.getForm().getDisplay().getOrder());
        return fieldList
                .stream()
                .sorted(orderComparator)
                .toList();
    }

    private List<List<UiField>> getFieldHierarchy(Model model) {
        return new LinkedList<>(getSectionFieldsList(model.getSections(), null));
    }

    private List<List<UiField>> getSectionFieldsList(List<Section> sections, List<UiField> existingFields) {
        List<List<UiField>> fields = new LinkedList<>();

        for (Section section : sections) {
            if (section.getSubSections() != null) {
                List<UiField> keepHierarchy = new LinkedList<>();
                if (existingFields != null) {
                    keepHierarchy.addAll(existingFields);
                }
                UiField falseField = new UiField();
                falseField.setName(section.getName());
                falseField.setId(section.getId());
                falseField.setLabel(StyledString.of(section.getName()));
                keepHierarchy.add(falseField);
                fields.addAll(getSectionFieldsList(section.getSubSections(), keepHierarchy));
            }
            if (section.getFields() != null) {
                List<UiField> sortedFields = sortFieldList(section.getFields());
                for (UiField field : sortedFields) {
                    if (field.isDeprecated())
                        logger.info("Field '{}' is deprecated.", field.getName());
                    fields.addAll(fieldsToLeaf(new LinkedList<>(existingFields), field));
                }
            }
        }
        return fields;
    }

    private List<List<UiField>> fieldsToLeaf(List<UiField> leafFieldList, UiField current) {
        List<List<UiField>> allLeafFieldLists = new LinkedList<>();
        if (leafFieldList == null) {
            leafFieldList = new LinkedList<>();
        }
        leafFieldList.add(current);
        if (current.getSubFields() != null && !current.getSubFields().isEmpty()) {
            if (current.getSubFields() != null) {
                List<UiField> currentSortedFields = sortFieldList(current.getSubFields());
                for (UiField field : currentSortedFields) {
                    List<UiField> list = new LinkedList<>(leafFieldList);
//                list.add(field);
                    allLeafFieldLists.addAll(fieldsToLeaf(list, field));
                }
            }
        } else {
            if (!leafFieldList.isEmpty()) {
                allLeafFieldLists.add(leafFieldList);
            }
        }
        return allLeafFieldLists;
    }

    private String getKey(List<UiField> fieldsToLeaf) {
        String key = "";

        for (UiField field : fieldsToLeaf) {
            String label = field.getLabel().getText();
            if (label == null) {
                label = field.getName();
            }
            key = String.format("%s%s%s", key, JOINING_SYMBOL, label);
        }

        if (key.startsWith(JOINING_SYMBOL)) {
            key = key.replaceFirst(JOINING_SYMBOL, "");
        }
        return key;
    }

    private void createValues(String key, List<UiField> fieldsToLeaf, JSONObject answer, int index) {
        List values = new LinkedList<>();
        if (key == null) {
            key = "";
        }
        Object temp = null;
        if (answer != null) {
            temp = JSONValue.parse(answer.toJSONString());
        }
        for (UiField field : fieldsToLeaf) {
            if (field.isDeprecated())
                return;
            String label;
            if (field.getLabel() != null && field.getLabel().getText() != null) {
                label = field.getLabel().getText();
            } else {
                label = field.getName();
            }
            key = String.format("%s%s%s", key, JOINING_SYMBOL, label);

            if (temp instanceof JSONObject && ((JSONObject) temp).containsKey(field.getName())) {
                temp = ((JSONObject) temp).get(field.getName());
                if (field.getTypeInfo().isMultiplicity() && field.getTypeInfo().getType().equals("composite") && temp != null) {
                    for (Object item : (JSONArray) temp) {
                        createValues(key, fieldsToLeaf.subList(fieldsToLeaf.indexOf(field) + 1, fieldsToLeaf.size()), (JSONObject) item, index);
                    }
                    return;
                }
            } else if (temp instanceof JSONArray) {
                temp = ((JSONArray) temp);
            } else {
                temp = null;
            }
        }

        if (temp != null) {
            if (fieldsToLeaf.get(fieldsToLeaf.size() - 1).getTypeInfo().isMultiplicity()) {
                for (Object item : (JSONArray) temp) {
                    if (item instanceof String) { // Filter out problematic data
                        values.add(item);
                    }
                }
            } else {
                values.add(temp);
            }
        }
        if (key.startsWith(JOINING_SYMBOL)) {
            key = key.replaceFirst(JOINING_SYMBOL, "");
        }
        this.csvBuilder.addValue(key, index, values.isEmpty() ? null : values);
    }
}
