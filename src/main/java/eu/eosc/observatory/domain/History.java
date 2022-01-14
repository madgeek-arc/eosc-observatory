package eu.eosc.observatory.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class History {

    List<HistoryEntry> history = new ArrayList<>();

    public History() {}

    public List<HistoryEntry> getHistory() {
        return history;
    }

    public void setHistory(List<HistoryEntry> history) {
        this.history = history;
    }

    public void addEntry(String userId, Date date, String chapterAnswerId, HistoryAction action) {
        history.add(new HistoryEntry(userId, date, chapterAnswerId, action));
    }

    public enum HistoryAction {
        CREATED,
        UPDATED,
        VALIDATED,
        INVALIDATED,
        PUBLISHED,
        UNPUBLISHED;
    }
}
