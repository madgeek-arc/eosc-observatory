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

    /**
     * The branch that was missing entirely: {@code addEditor} guarded on {@code latest.equals(editor)}
     * with no else, so a <em>different</em> editor was silently discarded. Only the first person to
     * touch an answer was ever recorded, and {@code updateHistory} then stamped everyone else's edits
     * with that person's identity and timestamp — breaking the audit in exactly the concurrent-edit
     * scenario the aggregation exists for.
     */
    @Test
    void addEditorRecordsADifferentEditor() {
        SurveyAnswerRevisionsAggregation aggregation = new SurveyAnswerRevisionsAggregation(createSurveyAnswer());
        aggregation.getEditors().clear();

        Editor alice = new Editor()
                .setUser("alice@example.org")
                .setRole("manager")
                .setUpdateDate(new Date(10_000L));
        Editor bob = new Editor()
                .setUser("bob@example.org")
                .setRole("manager")
                .setUpdateDate(new Date(12_000L));

        aggregation.addEditor(alice);
        aggregation.addEditor(bob);

        assertEquals(2, aggregation.getEditors().size());
        assertEquals("alice@example.org", aggregation.getEditors().get(0).getUser());
        assertEquals("bob@example.org", aggregation.getEditors().get(1).getUser());
    }

    @Test
    void addEditorTreatsTheSamePersonInADifferentRoleAsADistinctEditor() {
        SurveyAnswerRevisionsAggregation aggregation = new SurveyAnswerRevisionsAggregation(createSurveyAnswer());
        aggregation.getEditors().clear();

        aggregation.addEditor(new Editor()
                .setUser("alice@example.org")
                .setRole("contributor")
                .setUpdateDate(new Date(10_000L)));
        aggregation.addEditor(new Editor()
                .setUser("alice@example.org")
                .setRole("manager")
                .setUpdateDate(new Date(12_000L)));

        assertEquals(2, aggregation.getEditors().size());
    }

    /**
     * With more than one editor recorded, {@code updateHistory} joins them into {@code modifiedBy} —
     * which is why the purge scrub cannot use a whole-string equals against that field.
     */
    @Test
    void updateHistoryJoinsMultipleEditorsIntoModifiedBy() {
        SurveyAnswer surveyAnswer = createSurveyAnswer();
        SurveyAnswerRevisionsAggregation aggregation = new SurveyAnswerRevisionsAggregation(surveyAnswer);

        Revision revision = new Revision();
        revision.setField("section.question");
        revision.setValue("updated");
        revision.setAction(new Action().setType(Action.Type.ADD));

        aggregation.applyRevision(revision, new Editor()
                .setUser("alice@example.org")
                .setRole("manager")
                .setUpdateDate(new Date(10_000L)));
        aggregation.applyRevision(revision, new Editor()
                .setUser("bob@example.org")
                .setRole("manager")
                .setUpdateDate(new Date(12_000L)));

        assertEquals("alice@example.org,bob@example.org", surveyAnswer.getMetadata().getModifiedBy());
        // The entry timestamp tracks the most recent editor, not the first one.
        assertEquals(12_000L, surveyAnswer.getHistory().getEntries().getLast().getTime());
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
