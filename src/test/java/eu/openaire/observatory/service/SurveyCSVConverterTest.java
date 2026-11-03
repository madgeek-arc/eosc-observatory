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

import eu.openaire.observatory.datasets.ExternalValueProvider;
import eu.openaire.observatory.datasets.NoExternalValueProvider;
import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.SurveyAnswer;
import gr.uoa.di.madgik.catalogue.service.ModelService;
import gr.uoa.di.madgik.catalogue.ui.domain.Display;
import gr.uoa.di.madgik.catalogue.ui.domain.FieldType;
import gr.uoa.di.madgik.catalogue.ui.domain.Form;
import gr.uoa.di.madgik.catalogue.ui.domain.Model;
import gr.uoa.di.madgik.catalogue.ui.domain.Section;
import gr.uoa.di.madgik.catalogue.ui.domain.StyledString;
import gr.uoa.di.madgik.catalogue.ui.domain.TypeInfo;
import gr.uoa.di.madgik.catalogue.ui.domain.UiField;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression tests for the "extract to CSV" nested/repeatable question group bug: a repeatable
 * ("multiplicity" + "composite") {@link UiField} must be recursed into per repetition instead of being
 * dumped as raw JSON into a single cell.
 */
@ExtendWith(MockitoExtension.class)
class SurveyCSVConverterTest {

    @Mock
    private ModelService modelService;
    @Mock
    private SurveyService surveyService;
    @Mock
    private StakeholderService stakeholderService;
    @Mock
    private UserService userService;

    private SurveyCSVConverter converter;

    private static final Date FROM = new Date(0);
    private static final Date TO = new Date(Long.MAX_VALUE / 2);

    private void setUp() {
        setUp((fieldName, year) -> Map.of());
    }

    private void setUp(ExternalValueProvider externalValueProvider) {
        converter = new SurveyCSVConverter(modelService, surveyService, stakeholderService, userService, externalValueProvider);
    }

    /** Builds a leaf/composite {@link UiField} with the plumbing (typeInfo, form/display) createValues() relies on. */
    private static UiField field(String name, String label, FieldType type, boolean multiplicity, List<UiField> subFields) {
        UiField field = new UiField();
        field.setName(name);
        field.setLabel(StyledString.of(label));
        TypeInfo typeInfo = new TypeInfo();
        typeInfo.setType(type);
        typeInfo.setMultiplicity(multiplicity);
        field.setTypeInfo(typeInfo);
        Form form = new Form();
        Display display = new Display();
        display.setOrder(1);
        form.setDisplay(display);
        field.setForm(form);
        field.setSubFields(subFields);
        return field;
    }

    /** Same as {@link #field(String, String, FieldType, boolean, List)} with a kind ("paused", "external", ...). */
    private static UiField field(String name, String label, String kind, FieldType type, boolean multiplicity, List<UiField> subFields) {
        UiField field = field(name, label, type, multiplicity, subFields);
        field.setKind(kind);
        return field;
    }

    /** Wraps a single top-level field under a "Practices" section, matching how convertToCSV builds column keys. */
    private static Model model(UiField topLevelField) {
        Section fieldsSection = new Section();
        fieldsSection.setFields(List.of(topLevelField));

        Section practicesSection = new Section();
        practicesSection.setName("Practices");
        practicesSection.setSubSections(List.of(fieldsSection));

        Model model = new Model();
        model.setId("survey-1");
        model.setSections(List.of(practicesSection));
        return model;
    }

    private static SurveyAnswer answer(String stakeholderId, JSONObject data) {
        SurveyAnswer surveyAnswer = new SurveyAnswer();
        surveyAnswer.setStakeholderId(stakeholderId);
        surveyAnswer.setAnswer(data);
        surveyAnswer.getMetadata().setCreationDate(new Date());
        return surveyAnswer;
    }

    private void mockAnswers(Model model, SurveyAnswer... answers) {
        when(modelService.get(model.getId())).thenReturn(model);
        Set<Stakeholder> stakeholders = new HashSet<>();
        for (SurveyAnswer answer : answers) {
            Stakeholder stakeholder = new Stakeholder();
            stakeholder.setId(answer.getStakeholderId());
            stakeholder.setName("Stakeholder " + answer.getStakeholderId());
            stakeholders.add(stakeholder);
            when(stakeholderService.get(stakeholder.getId())).thenReturn(stakeholder);
            when(surveyService.getLatest(model.getId(), answer.getStakeholderId())).thenReturn(answer);
        }
        when(stakeholderService.getWithFilter("type", model.getType())).thenReturn(stakeholders);
    }

    @Test
    void convertToCSVExtractsScalarValueFromSingleRepetitionOfCompositeMultiplicityField() {
        setUp();

        // "Question89" mirrors the reported bug: itself multiplicity+composite, directly wrapping one leaf question.
        UiField leaf = field("Question89-1", "84. How many repositories offered long-term data preservation in your country in 2024?",
                FieldType.number, false, null);
        UiField question89 = field("Question89", "Question89", FieldType.composite, true, List.of(leaf));
        Model model = model(question89);

        JSONObject entry = new JSONObject();
        entry.put("Question89-1", "42");
        JSONArray repetitions = new JSONArray();
        repetitions.add(entry);
        // Sections correspond to a nesting level in the answer JSON too, not just in the UI schema.
        JSONObject practicesData = new JSONObject();
        practicesData.put("Question89", repetitions);
        JSONObject answerData = new JSONObject();
        answerData.put("Practices", practicesData);

        SurveyAnswer surveyAnswer = answer("sh-1", answerData);
        mockAnswers(model, surveyAnswer);

        String csv = converter.convertToCSV(model.getId(), false, false, FROM, TO);

        String expectedHeader = "Practices->Question89->84. How many repositories offered long-term data preservation in your country in 2024?";
        assertThat(csv).contains(expectedHeader);
        // The bug dumped the raw JSONArray/JSONObject into the cell instead of the scalar "42".
        assertThat(csv).doesNotContain("Question89-1");
        assertThat(csv).doesNotContain("{").doesNotContain("[");

        String value = cellValue(csv, expectedHeader);
        assertThat(value).isEqualTo("42");
    }

    @Test
    void convertToCSVSpillsMultipleRepetitionsIntoSiblingColumnsInsteadOfOneJsonBlob() {
        setUp();

        UiField description = field("Question55-1", "50.1. Please provide a short description of the use case",
                FieldType.largeText, false, null);
        UiField question55 = field("Question55", "Question55", FieldType.composite, true, List.of(description));
        Model model = model(question55);

        JSONObject firstEntry = new JSONObject();
        firstEntry.put("Question55-1", "First use case description");
        JSONObject secondEntry = new JSONObject();
        secondEntry.put("Question55-1", "Second use case description");
        JSONArray repetitions = new JSONArray();
        repetitions.add(firstEntry);
        repetitions.add(secondEntry);
        JSONObject practicesData = new JSONObject();
        practicesData.put("Question55", repetitions);
        JSONObject answerData = new JSONObject();
        answerData.put("Practices", practicesData);

        SurveyAnswer surveyAnswer = answer("sh-1", answerData);
        mockAnswers(model, surveyAnswer);

        String csv = converter.convertToCSV(model.getId(), false, false, FROM, TO);

        String baseHeader = "Practices->Question55->50.1. Please provide a short description of the use case";
        assertThat(csv).contains(baseHeader);
        assertThat(csv).doesNotContain("Question55-1");
        assertThat(csv).doesNotContain("{").doesNotContain("[");

        List<String> headers = headerRow(csv);
        List<String> siblingHeaders = headers.stream().filter(h -> h.startsWith(baseHeader) && !h.equals(baseHeader)).toList();
        assertThat(siblingHeaders).hasSize(1);

        assertThat(cellValue(csv, baseHeader)).isEqualTo("First use case description");
        assertThat(cellValue(csv, siblingHeaders.get(0))).isEqualTo("Second use case description");
    }

    @Test
    void convertToCSVExtractsArrayValueFromMultiplicityLeafInsideCompositeRepetition() {
        setUp();

        // "Question55-2-1" mirrors the reported bug: a multiplicity leaf (array-of-strings answer)
        // nested inside a single "Question55" composite repetition.
        UiField description = field("Question55-1", "50.1. Please provide a short description of the use case",
                FieldType.largeText, false, null);
        UiField repositories = field("Question55-2-1", "50.2.1. Which repositories do you use?",
                FieldType.checkbox, true, null);
        UiField question55 = field("Question55", "Question55", FieldType.composite, true, List.of(description, repositories));
        Model model = model(question55);

        JSONObject entry = new JSONObject();
        entry.put("Question55-1", "Researchers publish in open access repositories...");
        JSONArray urls = new JSONArray();
        urls.add("http://example.org/");
        urls.add("https://example.com/repository/");
        urls.add("https://www.example.org/archive/");
        entry.put("Question55-2-1", urls);
        JSONArray repetitions = new JSONArray();
        repetitions.add(entry);
        JSONObject practicesData = new JSONObject();
        practicesData.put("Question55", repetitions);
        JSONObject answerData = new JSONObject();
        answerData.put("Practices", practicesData);

        SurveyAnswer surveyAnswer = answer("sh-1", answerData);
        mockAnswers(model, surveyAnswer);

        String csv = converter.convertToCSV(model.getId(), false, false, FROM, TO);

        String expectedHeader = "Practices->Question55->50.2.1. Which repositories do you use?";
        assertThat(csv).contains(expectedHeader);
        // The bug silently dropped this value (item instanceof String was false against the
        // un-descended repetition objects), instead of extracting the array's string items.
        String value = cellValue(csv, expectedHeader);
        assertThat(value).isNotBlank();
        assertThat(value).doesNotContain("{").doesNotContain("[");
        assertThat(value).contains("http://example.org/").contains("https://example.com/repository/");
    }

    /** Wraps the answer of a single top-level question under the "Practices" section. */
    private static JSONObject practicesAnswer(String questionName, Object questionData) {
        JSONObject practicesData = new JSONObject();
        practicesData.put(questionName, questionData);
        JSONObject answerData = new JSONObject();
        answerData.put("Practices", practicesData);
        return answerData;
    }

    @Test
    void convertToCSVTagsEveryColumnOfExternalQuestionBeforeItsOwnLabel() {
        setUp();

        // mirrors Question69: the question and its first sub-question are external, the rest have no kind
        UiField count = field("Question69-0", "64. How many open data sets?", "external", FieldType.number, false, null);
        UiField agree = field("Question69-1", "64.1. Do you agree?", FieldType.radio, false, null);
        UiField question69 = field("Question69", "64. How many open data sets?", "external", FieldType.composite, false, List.of(count, agree));
        Model model = model(question69);

        JSONObject data = new JSONObject();
        data.put("Question69-0", "10");
        data.put("Question69-1", "Yes");
        mockAnswers(model, answer("sh-1", practicesAnswer("Question69", data)));

        String csv = converter.convertToCSV(model.getId(), false, false, FROM, TO);

        String countHeader = "Practices->64. How many open data sets?->(External) 64. How many open data sets?";
        String agreeHeader = "Practices->64. How many open data sets?->(External) 64.1. Do you agree?";
        assertThat(headerRow(csv)).contains(countHeader, agreeHeader);
        assertThat(cellValue(csv, countHeader)).isEqualTo("10");
        assertThat(cellValue(csv, agreeHeader)).isEqualTo("Yes");
        // the tag goes only on the column's own question, never on the parent
        assertThat(headerRow(csv)).noneMatch(h -> h.startsWith("Practices->(External)"));
    }

    @Test
    void convertToCSVTagsEveryColumnOfPausedQuestionOnce() {
        setUp();

        // mirrors Question26: only the question is paused, its sub-questions (one inside an unlabelled group) have no kind
        UiField policy = field("Question26-0", "21. Is there a national policy?", FieldType.radio, false, null);
        UiField mandatory = field("Question26-1", "21.1. Is this policy mandatory?", FieldType.radio, false, null);
        UiField group = field("Question26-1-0", null, FieldType.composite, false, List.of(mandatory));
        UiField question26 = field("Question26", "21. Is there a national policy?", "paused", FieldType.composite, false, List.of(policy, group));
        Model model = model(question26);

        mockAnswers(model, answer("sh-1", practicesAnswer("Question26", new JSONObject())));

        String csv = converter.convertToCSV(model.getId(), false, false, FROM, TO);

        assertThat(headerRow(csv)).contains(
                "Practices->21. Is there a national policy?->(Paused) 21. Is there a national policy?",
                "Practices->21. Is there a national policy?->Question26-1-0->(Paused) 21.1. Is this policy mandatory?");
        assertThat(headerRow(csv))
                .filteredOn(h -> h.startsWith("Practices->"))
                .allMatch(h -> h.indexOf("(Paused) ") == h.lastIndexOf("(Paused) "));
    }

    @Test
    void convertToCSVTagsOnlyPausedWhenExternalQuestionIsInsidePausedQuestion() {
        setUp();

        // mirrors Question77: the question is paused and its first sub-question is external
        UiField count = field("Question77-0", "72. How many services?", "external", FieldType.number, false, null);
        UiField question77 = field("Question77", "72. How many services?", "paused", FieldType.composite, false, List.of(count));
        Model model = model(question77);

        mockAnswers(model, answer("sh-1", practicesAnswer("Question77", new JSONObject())));

        String csv = converter.convertToCSV(model.getId(), false, false, FROM, TO);

        assertThat(headerRow(csv)).contains("Practices->72. How many services?->(Paused) 72. How many services?");
        assertThat(csv).doesNotContain("(External)");
    }

    @Test
    void convertToCSVTagsOnlyThePausedSubQuestionOfAnUnlabelledGroup() {
        setUp();

        // mirrors Question65: the group has no kind and no label text, only its first sub-question is paused
        UiField count = field("Question65-0", "60. How many FAIR data sets?", "paused", FieldType.number, false, null);
        UiField clarify = field("Question65-1", "60.1. Please clarify", FieldType.largeText, false, null);
        UiField question65 = field("Question65", null, FieldType.composite, false, List.of(count, clarify));
        Model model = model(question65);

        mockAnswers(model, answer("sh-1", practicesAnswer("Question65", new JSONObject())));

        String csv = converter.convertToCSV(model.getId(), false, false, FROM, TO);

        assertThat(headerRow(csv)).contains(
                "Practices->Question65->(Paused) 60. How many FAIR data sets?",
                "Practices->Question65->60.1. Please clarify");
    }

    @Test
    void convertToCSVTagsEveryRepetitionColumnOfRepeatableGroupInsidePausedQuestion() {
        setUp();

        // mirrors Question75: a repeatable group of use cases inside a paused question
        UiField description = field("Question75-1", "70.1. Please describe the use case", FieldType.largeText, false, null);
        UiField useCases = field("Question75-1-0", null, FieldType.composite, true, List.of(description));
        UiField question75 = field("Question75", "70. Are there use cases?", "paused", FieldType.composite, false, List.of(useCases));
        Model model = model(question75);

        JSONObject first = new JSONObject();
        first.put("Question75-1", "First use case");
        JSONObject second = new JSONObject();
        second.put("Question75-1", "Second use case");
        JSONArray repetitions = new JSONArray();
        repetitions.add(first);
        repetitions.add(second);
        JSONObject data = new JSONObject();
        data.put("Question75-1-0", repetitions);
        mockAnswers(model, answer("sh-1", practicesAnswer("Question75", data)));

        String csv = converter.convertToCSV(model.getId(), false, false, FROM, TO);

        String baseHeader = "Practices->70. Are there use cases?->Question75-1-0->(Paused) 70.1. Please describe the use case";
        List<String> repetitionHeaders = headerRow(csv).stream().filter(h -> h.startsWith(baseHeader)).toList();
        assertThat(repetitionHeaders).hasSize(2);
        assertThat(repetitionHeaders).allMatch(h -> h.indexOf("(Paused) ") == h.lastIndexOf("(Paused) "));
        assertThat(cellValue(csv, repetitionHeaders.get(0))).isEqualTo("First use case");
        assertThat(cellValue(csv, repetitionHeaders.get(1))).isEqualTo("Second use case");
    }

    @Test
    void convertToCSVKeepsHeadersOfQuestionsWithoutPausedOrExternalKind() {
        setUp();

        UiField amount = field("Question68-0", "63. How much did your country invest?", "question", FieldType.number, false, null);
        UiField question68 = field("Question68", "Question68", FieldType.composite, false, List.of(amount));
        Model model = model(question68);

        mockAnswers(model, answer("sh-1", practicesAnswer("Question68", new JSONObject())));

        String csv = converter.convertToCSV(model.getId(), false, false, FROM, TO);

        assertThat(headerRow(csv)).contains("Practices->Question68->63. How much did your country invest?");
        assertThat(csv).doesNotContain("(Paused)").doesNotContain("(External)");
    }

    private static Stakeholder stakeholder(String id, String name, String country) {
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setId(id);
        stakeholder.setName(name);
        stakeholder.setCountry(country);
        return stakeholder;
    }

    private void mockStakeholders(Model model, Stakeholder... stakeholders) {
        when(modelService.get(model.getId())).thenReturn(model);
        when(stakeholderService.getWithFilter("type", model.getType())).thenReturn(new HashSet<>(Arrays.asList(stakeholders)));
    }

    /** Mirrors Question57 and Question77: an external question and an external question inside a paused one. */
    private static Model externalModel() {
        UiField publications = field("Question57-0", "52. How many open access publications?", FieldType.number, false, null);
        UiField question57 = field("Question57", "52. How many open access publications?", "external", FieldType.composite, false, List.of(publications));
        UiField services = field("Question77-0", "72. How many services?", "external", FieldType.number, false, null);
        UiField question77 = field("Question77", "72. How many services?", "paused", FieldType.composite, false, List.of(services));
        UiField policy = field("Question26-0", "21. Is there a national policy?", FieldType.radio, false, null);
        UiField question26 = field("Question26", "21. Is there a national policy?", FieldType.composite, false, List.of(policy));

        Section fieldsSection = new Section();
        fieldsSection.setFields(List.of(question57, question77, question26));
        Section practicesSection = new Section();
        practicesSection.setName("Practices");
        practicesSection.setSubSections(List.of(fieldsSection));

        Model model = new Model();
        model.setId("m-eosc-sb-2025");
        model.setSections(List.of(practicesSection));
        return model;
    }

    private static final String PUBLICATIONS_HEADER =
            "Practices->52. How many open access publications?->(External) 52. How many open access publications?";
    private static final String SERVICES_HEADER = "Practices->72. How many services?->(Paused) 72. How many services?";

    private static String cellValue(String csv, String header, int row) {
        int columnIndex = headerRow(csv).indexOf(header);
        assertThat(columnIndex).as("header '%s' not found in CSV output", header).isGreaterThanOrEqualTo(0);
        return csv.split("\n")[row].split("\t", -1)[columnIndex];
    }

    @Test
    void convertExternalToCSVKeepsOnlyExternalQuestionsFilledByCountry() {
        setUp((fieldName, year) -> "Question57-0".equals(fieldName) && "2024".equals(year)
                ? Map.of("GR", "1200", "FR", "5300")
                : Map.of());
        Model model = externalModel();
        mockStakeholders(model, stakeholder("sh-country-GR", "Greece", "GR"), stakeholder("sh-country-FR", "France", "FR"));

        String csv = converter.convertExternalToCSV(model.getId(), "2024");

        assertThat(headerRow(csv)).contains("Stakeholder Id", "Stakeholder Names", "Country", PUBLICATIONS_HEADER, SERVICES_HEADER);
        assertThat(csv).doesNotContain("21. Is there a national policy?");
        // rows are sorted by stakeholder name
        assertThat(cellValue(csv, "Stakeholder Names", 1)).isEqualTo("France");
        assertThat(cellValue(csv, PUBLICATIONS_HEADER, 1)).isEqualTo("5300");
        assertThat(cellValue(csv, "Stakeholder Names", 2)).isEqualTo("Greece");
        assertThat(cellValue(csv, PUBLICATIONS_HEADER, 2)).isEqualTo("1200");
        assertThat(cellValue(csv, SERVICES_HEADER, 2)).isEmpty();
        // the values come from the external source, the answers are never read
        verify(surveyService, never()).getLatest(any(), any());
    }

    @Test
    void convertExternalToCSVLeavesCellsEmptyWithoutValueOrCountry() {
        setUp((fieldName, year) -> Map.of("GR", "1200"));
        Model model = externalModel();
        mockStakeholders(model, stakeholder("sh-country-IT", "Italy", "IT"), stakeholder("sh-other", "Other", null));

        String csv = converter.convertExternalToCSV(model.getId(), "2024");

        assertThat(cellValue(csv, PUBLICATIONS_HEADER, 1)).isEmpty();
        assertThat(cellValue(csv, "Country", 2)).isEmpty();
        assertThat(cellValue(csv, PUBLICATIONS_HEADER, 2)).isEmpty();
        assertThat(csv).doesNotContain("null");
    }

    @Test
    void convertExternalToCSVHasHeadersAndEmptyCellsWithThePlaceholderProvider() {
        setUp(new NoExternalValueProvider());
        Model model = externalModel();
        mockStakeholders(model, stakeholder("sh-country-GR", "Greece", "GR"));

        String csv = converter.convertExternalToCSV(model.getId(), "2024");

        assertThat(csv.split("\n")).hasSize(2);
        assertThat(cellValue(csv, "Country", 1)).isEqualTo("GR");
        assertThat(cellValue(csv, PUBLICATIONS_HEADER, 1)).isEmpty();
        assertThat(cellValue(csv, SERVICES_HEADER, 1)).isEmpty();
    }

    @Test
    void convertExternalToCSVKeepsDeprecatedExternalQuestions() {
        setUp((fieldName, year) -> Map.of("GR", "35000"));
        // mirrors Question1: deprecated in the 2025 model, but its value still comes from Eurostat
        UiField researchers = field("Question1-0", "1. How many researchers (FTE)?", "external", FieldType.string, false, null);
        researchers.setDeprecated(true);
        UiField question1 = field("Question1", "1. How many researchers (FTE)?", FieldType.composite, false, List.of(researchers));
        question1.setDeprecated(true);
        Model model = model(question1);
        mockStakeholders(model, stakeholder("sh-country-GR", "Greece", "GR"));

        String csv = converter.convertExternalToCSV(model.getId(), "2024");

        assertThat(cellValue(csv, "Practices->1. How many researchers (FTE)?->(External) 1. How many researchers (FTE)?", 1))
                .isEqualTo("35000");
    }

    @Test
    void convertExternalToCSVKeepsColumnsWithTheSameLabelApart() {
        setUp((fieldName, year) -> Map.of("GR", fieldName));
        UiField first = field("Question10-0", "How many?", "external", FieldType.number, false, null);
        UiField second = field("Question10-1", "How many?", "external", FieldType.number, false, null);
        UiField question10 = field("Question10", "Question10", FieldType.composite, false, List.of(first, second));
        Model model = model(question10);
        mockStakeholders(model, stakeholder("sh-country-GR", "Greece", "GR"));

        String csv = converter.convertExternalToCSV(model.getId(), "2024");

        assertThat(cellValue(csv, "Practices->Question10->(External) How many?", 1)).isEqualTo("Question10-0");
        assertThat(cellValue(csv, "Practices->Question10->(External) How many? (Question10-1)", 1)).isEqualTo("Question10-1");
    }

    @Test
    void convertExternalToCSVHasOnlyHeadersWithoutStakeholders() {
        setUp();
        Model model = externalModel();
        mockStakeholders(model);

        String csv = converter.convertExternalToCSV(model.getId(), "2024");

        assertThat(csv.split("\n")).hasSize(1);
        assertThat(headerRow(csv)).contains(PUBLICATIONS_HEADER);
    }

    private static List<String> headerRow(String csv) {
        String headerLine = csv.split("\n")[0];
        return Arrays.asList(headerLine.split("\t", -1));
    }

    private static String cellValue(String csv, String header) {
        String[] lines = csv.split("\n");
        List<String> headers = headerRow(csv);
        int columnIndex = headers.indexOf(header);
        assertThat(columnIndex).as("header '%s' not found in CSV output", header).isGreaterThanOrEqualTo(0);
        String[] dataRow = lines[1].split("\t", -1);
        return dataRow[columnIndex];
    }
}
