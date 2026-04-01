package eu.openaire.observatory.domain;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SurveyAnswerRevisionsAggregationTest {

    @Test
    void constructorInitializesHistoryAndDefaults() {
        SurveyAnswer surveyAnswer = new SurveyAnswer();

        SurveyAnswerRevisionsAggregation aggregation = new SurveyAnswerRevisionsAggregation(surveyAnswer);

        assertSame(surveyAnswer, aggregation.getSurveyAnswer());
        assertNotNull(aggregation.getCreated());
        assertEquals(1, surveyAnswer.getHistory().getEntries().size());
        assertEquals(History.HistoryAction.UPDATED, surveyAnswer.getHistory().getEntries().getFirst().getAction());
    }

    @Test
    void applyRevisionAddsMissingFieldAndUpdatesHistoryMetadata() {
        SurveyAnswer surveyAnswer = createSurveyAnswer();
        SurveyAnswerRevisionsAggregation aggregation = new SurveyAnswerRevisionsAggregation(surveyAnswer);
        aggregation.getSurveyAnswer().getHistory().getEntries().clear();
        aggregation.getSurveyAnswer().getHistory().addEntry("seed", "manager", "", new Date(1_000L), History.HistoryAction.UPDATED);

        Revision revision = new Revision();
        revision.setField("section.question");
        revision.setValue("updated");
        revision.setAction(new Action().setType(Action.Type.ADD));

        Editor editor = new Editor()
                .setUser("editor@example.org")
                .setRole("manager")
                .setUpdateDate(new Date(5_000L));

        aggregation.applyRevision(revision, editor);

        JSONObject section = (JSONObject) aggregation.getSurveyAnswer().getAnswer().get("section");
        assertEquals("updated", section.get("question"));
        assertEquals(1, aggregation.getRevisions().size());
        assertEquals(editor, aggregation.getEditors().getFirst());
        assertEquals("editor@example.org", aggregation.getSurveyAnswer().getMetadata().getModifiedBy());
        assertEquals(editor.getUpdateDate(), aggregation.getSurveyAnswer().getMetadata().getModificationDate());
        assertEquals(1, aggregation.getSurveyAnswer().getHistory().getEntries().size());
        assertEquals(editor.getUpdateDate().getTime(), aggregation.getSurveyAnswer().getHistory().getEntries().getFirst().getTime());
        assertEquals(1, aggregation.getSurveyAnswer().getHistory().getEntries().getFirst().getEditors().size());
    }

    @Test
    void addEditorMergesRepeatedUpdatesFromSameEditorWithinOneMinute() {
        SurveyAnswerRevisionsAggregation aggregation = new SurveyAnswerRevisionsAggregation(createSurveyAnswer());
        aggregation.getEditors().clear();

        Editor first = new Editor()
                .setUser("editor@example.org")
                .setRole("manager")
                .setUpdateDate(new Date(10_000L));
        Editor repeated = new Editor()
                .setUser("editor@example.org")
                .setRole("manager")
                .setUpdateDate(new Date(40_000L));

        aggregation.addEditor(first);
        aggregation.addEditor(repeated);

        assertEquals(1, aggregation.getEditors().size());
        assertEquals(repeated.getUpdateDate(), aggregation.getEditors().getFirst().getUpdateDate());
    }

    @Test
    void addEditorKeepsSeparateEntriesAfterOneMinuteGap() {
        SurveyAnswerRevisionsAggregation aggregation = new SurveyAnswerRevisionsAggregation(createSurveyAnswer());
        aggregation.getEditors().clear();

        aggregation.addEditor(new Editor()
                .setUser("editor@example.org")
                .setRole("manager")
                .setUpdateDate(new Date(10_000L)));
        aggregation.addEditor(new Editor()
                .setUser("editor@example.org")
                .setRole("manager")
                .setUpdateDate(new Date(80_001L)));

        assertEquals(2, aggregation.getEditors().size());
    }

    private SurveyAnswer createSurveyAnswer() {
        SurveyAnswer surveyAnswer = new SurveyAnswer();
        surveyAnswer.setMetadata(new Metadata());
        surveyAnswer.getMetadata().setModifiedBy("initial");
        surveyAnswer.getMetadata().setModificationDate(new Date(0L));
        surveyAnswer.setAnswer(new JSONObject());
        return surveyAnswer;
    }
}
