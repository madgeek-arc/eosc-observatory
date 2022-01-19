package eu.eosc.observatory.domain;

import eu.eosc.observatory.service.Identifiable;
import org.json.simple.JSONObject;

public class ChapterAnswer implements Identifiable<String> {

    private String chapterId;
    private JSONObject answer;
    private Metadata metadata;

    public ChapterAnswer() {
        answer = new JSONObject();
    }

    public ChapterAnswer(String chapterAnswerId, String chapterId, Metadata metadata) {
        this.answer = new JSONObject();
        this.setId(chapterAnswerId);
        this.chapterId = chapterId;
        this.metadata = metadata;
    }

    @Override
    public String getId() {
        return answer.get("id").toString();
    }

    @Override
    public void setId(String id) {
        this.answer.put("id", id);
    }

    public String getChapterId() {
        return chapterId;
    }

    public void setChapterId(String chapterId) {
        this.chapterId = chapterId;
    }

    public JSONObject getAnswer() {
        return answer;
    }

    public void setAnswer(JSONObject answer) {
        this.answer = answer;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }
}
