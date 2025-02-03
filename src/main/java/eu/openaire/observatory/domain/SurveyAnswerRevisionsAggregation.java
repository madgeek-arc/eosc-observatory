/**
 * Copyright 2021-2025 OpenAIRE AMKE
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
package eu.openaire.observatory.domain;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import eu.openaire.observatory.utils.JSONObjectUtils;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class SurveyAnswerRevisionsAggregation implements Serializable {

    private SurveyAnswer surveyAnswer;
    private List<Revision> revisions;
    private final List<Editor> editors = new ArrayList<>();
    private final Date created = new Date();

//    private static final Configuration conf = Configuration.defaultConfiguration()
//            .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);

    public SurveyAnswerRevisionsAggregation(SurveyAnswer surveyAnswer) {
        this.surveyAnswer = surveyAnswer;
        this.surveyAnswer.getHistory().getEntries().add(new HistoryEntry(editors, null, created.getTime(), History.HistoryAction.UPDATED));
        this.revisions = new ArrayList<>();
    }

    public void applyRevision(Revision revision, Editor editor) {
        JSONObject ans = this.getSurveyAnswer().getAnswer();

        switch (revision.getAction().getType()) {
            case ADD -> ans = JSONObjectUtils.add(revision.getField(), revision.getValue(), ans);
            case UPDATE -> {
                try {
                    ans = JsonPath.parse(ans).set(revision.getField(), revision.getValue()).json();
                } catch (PathNotFoundException e) {
                    ans = JSONObjectUtils.add(revision.getField(), revision.getValue(), ans);
                }
            }
            case DELETE -> ans = JsonPath.parse(ans).delete(revision.getField()).json();
            case MOVE -> {
                String destination = revision.getField().replaceAll("\\[\\d+\\]$", "[" + revision.getAction().getIndex() + "]");
                Object from = JsonPath.parse(ans).read(revision.getField());
                Object to = JsonPath.parse(ans).read(destination);
                ans = JsonPath.parse(ans).set(revision.getField(), to).json();
                ans = JsonPath.parse(ans).set(destination, from).json();
            }
        }

        this.getSurveyAnswer().setAnswer(ans);
        this.getRevisions().add(revision);
        this.addEditor(editor);
        this.updateHistory();
    }

    private void updateHistory() {
        Editor lastEditor = this.editors.get(this.editors.size() - 1);

        List<HistoryEntry> historyEntryList = surveyAnswer.getHistory().getEntries();
        HistoryEntry entry = historyEntryList.get(historyEntryList.size() - 1);
        entry.setTime(lastEditor.getUpdateDate().getTime());
        entry.setEditors(editors);
        historyEntryList.set(historyEntryList.size() - 1, entry);
        surveyAnswer.getMetadata().setModifiedBy(String.join(", ", editors.stream().map(Editor::getUser).collect(Collectors.joining(","))));
        surveyAnswer.getMetadata().setModificationDate(lastEditor.getUpdateDate());
    }

    public SurveyAnswer getSurveyAnswer() {
        return surveyAnswer;
    }

    public void setSurveyAnswer(SurveyAnswer surveyAnswer) {
        this.surveyAnswer = surveyAnswer;
    }

    public List<Revision> getRevisions() {
        return revisions;
    }

    public void setRevisions(List<Revision> revisions) {
        this.revisions = revisions;
    }

    public List<Editor> getEditors() {
        return editors;
    }

    /**
     * <p>Adds Editor to list when the user is different or when a certain amount of time has elapsed
     * before the user's last update.</p>
     *
     * @param editor
     * @return
     */
    public SurveyAnswerRevisionsAggregation addEditor(Editor editor) {
        if (!this.editors.isEmpty()) {
            Editor latest = this.editors.get(this.editors.size() - 1);
            if (latest.equals(editor)) {
                if (editor.getUpdateDate().getTime() - latest.getUpdateDate().getTime() > 60_000) {
                    this.editors.add(editor);
                } else {
                    latest.setUpdateDate(editor.getUpdateDate());
                }
            }
        } else {
            this.editors.add(editor);
        }
        return this;
    }

    public Date getCreated() {
        return created;
    }
}
