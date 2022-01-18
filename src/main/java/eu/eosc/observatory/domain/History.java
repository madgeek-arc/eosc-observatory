package eu.eosc.observatory.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class History {

    List<HistoryEntry> entries = new ArrayList<>();

    public History() {}

    public List<HistoryEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<HistoryEntry> entries) {
        this.entries = entries;
    }

    public void addEntry(String userId, Date date, String chapterId, HistoryAction action) {
        entries.add(new HistoryEntry(userId, date.getTime(), chapterId, action));
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
