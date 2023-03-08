package eu.eosc.observatory.dto;

import eu.eosc.observatory.domain.History;
import eu.eosc.observatory.domain.HistoryEntry;
import eu.eosc.observatory.domain.User;

public class HistoryEntryDTO {

    String email;
    String fullname;
    long time;
    History.HistoryAction action;
    String resourceId;
    String version;

    public HistoryEntryDTO() {
        // no-arg constructor
    }

    public static HistoryEntryDTO of(HistoryEntry historyEntry, User user) {
        HistoryEntryDTO entry = new HistoryEntryDTO();
        if (!historyEntry.getUserId().equals(user.getId())) {
            throw new UnsupportedOperationException("wrong user id");
        }
        entry.setEmail(user.getEmail());
        entry.setFullname(user.getFullname());
        entry.setAction(historyEntry.getAction());
        entry.setTime(historyEntry.getTime());
        return entry;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
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

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
