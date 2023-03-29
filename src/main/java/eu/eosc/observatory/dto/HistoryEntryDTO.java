package eu.eosc.observatory.dto;

import eu.eosc.observatory.domain.History;
import eu.eosc.observatory.domain.HistoryEntry;
import eu.eosc.observatory.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoryEntryDTO {

    private static final Logger logger = LoggerFactory.getLogger(HistoryEntryDTO.class);

    String email;
    String fullname;
    long time;
    HistoryActionDTO action;
    String resourceId;
    String version;

    public HistoryEntryDTO() {
        // no-arg constructor
    }

    public static HistoryEntryDTO of(HistoryEntry historyEntry, User user) {
        HistoryEntryDTO entry = new HistoryEntryDTO();
        if (!historyEntry.getUserId().equals(user.getId())) {
            logger.error("wrong user id");
        }
        entry.setEmail(user.getEmail());
        entry.setFullname(user.getFullname());
        entry.setAction(HistoryActionDTO.of(historyEntry.getAction(), historyEntry.getRegistryVersion()));
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

    public HistoryActionDTO getAction() {
        return action;
    }

    public void setAction(HistoryActionDTO action) {
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
