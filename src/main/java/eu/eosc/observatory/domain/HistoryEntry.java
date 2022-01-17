package eu.eosc.observatory.domain;

public class HistoryEntry {

    String userId;
    long time;
    String chapterModified;
    History.HistoryAction action;

    public HistoryEntry() {}

    public HistoryEntry(String userId, long time, String chapterModified, History.HistoryAction action) {
        this.userId = userId;
        this.time = time;
        this.chapterModified = chapterModified;
        this.action = action;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getDate() {
        return time;
    }

    public void setDate(long time) {
        this.time = time;
    }

    public History.HistoryAction getAction() {
        return action;
    }

    public void setAction(History.HistoryAction action) {
        this.action = action;
    }
}
