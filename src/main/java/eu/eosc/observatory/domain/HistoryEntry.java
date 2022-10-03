package eu.eosc.observatory.domain;

public class HistoryEntry {

    String userId;
    long time;
    History.HistoryAction action;

    public HistoryEntry() {}

    public HistoryEntry(String userId, long time, History.HistoryAction action) {
        this.userId = userId;
        this.time = time;
        this.action = action;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public History.HistoryAction getAction() {
        return action;
    }

    public void setAction(History.HistoryAction action) {
        this.action = action;
    }
}
