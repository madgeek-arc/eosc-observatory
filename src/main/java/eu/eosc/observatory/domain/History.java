package eu.eosc.observatory.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class History implements Serializable {

    List<HistoryEntry> entries = new ArrayList<>();

    public History() {
        // no-arg constructor
    }

    public List<HistoryEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<HistoryEntry> entries) {
        this.entries = entries;
    }

    public void addEntry(String userId, String userRole, String comment, Date date, HistoryAction action) {
        entries.add(new HistoryEntry(userId, userRole, comment, date.getTime(), action));
    }

    public void addEntry(String userId, String userRole, String comment, Date date, HistoryAction action, String version) {
        entries.add(new HistoryEntry(userId, userRole, comment, date.getTime(), action, version));
    }

    public enum HistoryAction {
        CREATED,
        IMPORTED,
        UPDATED,
        VALIDATED,
        INVALIDATED,
        PUBLISHED,
        UNPUBLISHED,
        RESTORED;
    }
}
