package eu.openaire.observatory.domain;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SurveyAnswerRevisionsAggregationTest {

    @Test
    void constructorDoesNotAddHistoryEntryUntilFirstRevision() {
        SurveyAnswer surveyAnswer = new SurveyAnswer();

        SurveyAnswerRevisionsAggregation aggregation = new SurveyAnswerRevisionsAggregation(surveyAnswer);

        assertSame(surveyAnswer, aggregation.getSurveyAnswer());
        assertNotNull(aggregation.getCreated());
        assertEquals(0, surveyAnswer.getHistory().getEntries().size());
    }

    @Test
    void applyRevisionAddsMissingFieldAndUpdatesHistoryMetadata() {
        SurveyAnswer surveyAnswer = createSurveyAnswer();
        surveyAnswer.getHistory().addEntry("seed", "manager", "", new Date(1_000L), History.HistoryAction.UPDATED);
        SurveyAnswerRevisionsAggregation aggregation = new SurveyAnswerRevisionsAggregation(surveyAnswer);

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
        // the pre-existing "seed" entry is untouched; applyRevision appends its own new entry
        assertEquals(2, aggregation.getSurveyAnswer().getHistory().getEntries().size());
        assertEquals(History.HistoryAction.UPDATED, aggregation.getSurveyAnswer().getHistory().getEntries().getLast().getAction());
        assertEquals(editor.getUpdateDate().getTime(), aggregation.getSurveyAnswer().getHistory().getEntries().getLast().getTime());
        assertEquals(1, aggregation.getSurveyAnswer().getHistory().getEntries().getLast().getEditors().size());

        // a second revision from the same aggregation mutates its own entry in place rather than adding another
        Editor secondEditor = new Editor()
                .setUser("editor@example.org")
                .setRole("manager")
                .setUpdateDate(new Date(130_000L));
        aggregation.applyRevision(revision, secondEditor);
        assertEquals(2, aggregation.getSurveyAnswer().getHistory().getEntries().size());
        assertEquals(secondEditor.getUpdateDate().getTime(), aggregation.getSurveyAnswer().getHistory().getEntries().getLast().getTime());
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

    @Test
    void addEditorAppendsDifferentConcurrentEditor() {
        SurveyAnswerRevisionsAggregation aggregation = new SurveyAnswerRevisionsAggregation(createSurveyAnswer());
        aggregation.getEditors().clear();

        Editor first = new Editor()
                .setUser("alice@example.org")
                .setRole("manager")
                .setUpdateDate(new Date(10_000L));
        Editor different = new Editor()
                .setUser("bob@example.org")
                .setRole("contributor")
                .setUpdateDate(new Date(10_500L));

        aggregation.addEditor(first);
        aggregation.addEditor(different);

        assertEquals(2, aggregation.getEditors().size());
        assertEquals("alice@example.org", aggregation.getEditors().get(0).getUser());
        assertEquals("bob@example.org", aggregation.getEditors().get(1).getUser());
    }

    @Test
    void applyRevisionDeleteOfMissingFieldDoesNotThrowAndStillRecordsHistory() {
        SurveyAnswer surveyAnswer = createSurveyAnswer();
        SurveyAnswerRevisionsAggregation aggregation = new SurveyAnswerRevisionsAggregation(surveyAnswer);

        Revision revision = new Revision();
        revision.setField("section.missing");
        revision.setAction(new Action().setType(Action.Type.DELETE));

        Editor editor = new Editor()
                .setUser("editor@example.org")
                .setRole("manager")
                .setUpdateDate(new Date(5_000L));

        aggregation.applyRevision(revision, editor);

        assertEquals(1, aggregation.getRevisions().size());
        assertEquals(1, aggregation.getSurveyAnswer().getHistory().getEntries().size());
        assertEquals(1, aggregation.getSurveyAnswer().getHistory().getEntries().getFirst().getEditors().size());
    }

    @Test
    void applyRevisionMoveOfMissingFieldDoesNotThrowAndStillRecordsHistory() {
        SurveyAnswer surveyAnswer = createSurveyAnswer();
        SurveyAnswerRevisionsAggregation aggregation = new SurveyAnswerRevisionsAggregation(surveyAnswer);

        Revision revision = new Revision();
        revision.setField("section.items[0]");
        revision.setAction(new Action().setType(Action.Type.MOVE).setIndex(1));

        Editor editor = new Editor()
                .setUser("editor@example.org")
                .setRole("manager")
                .setUpdateDate(new Date(5_000L));

        aggregation.applyRevision(revision, editor);

        assertEquals(1, aggregation.getRevisions().size());
        assertEquals(1, aggregation.getSurveyAnswer().getHistory().getEntries().size());
        assertEquals(1, aggregation.getSurveyAnswer().getHistory().getEntries().getFirst().getEditors().size());
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
