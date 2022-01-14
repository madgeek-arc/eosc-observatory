package eu.eosc.observatory.domain;

import java.util.Date;

public class HistoryEntry {

    String userId;
    Date date;
    String chapterModified;
    History.HistoryAction action;

    public HistoryEntry() {}

    public HistoryEntry(String userId, Date date, History.HistoryAction action) {
        this.userId = userId;
        this.date = date;
        this.action = action;
    }

    public HistoryEntry(String userId, Date date, String chapterModified, History.HistoryAction action) {
        this.userId = userId;
        this.date = date;
        this.chapterModified = chapterModified;
        this.action = action;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public History.HistoryAction getAction() {
        return action;
    }

    public void setAction(History.HistoryAction action) {
        this.action = action;
    }
}
