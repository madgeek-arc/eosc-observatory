package eu.eosc.observatory.domain;

import java.io.Serializable;

public class HistoryEntry implements Serializable {

    String userId;
    String userRole;
    String comment;
    long time;
    History.HistoryAction action;
    String registryVersion;

    public HistoryEntry() {
        // no-arg constructor
    }

    public HistoryEntry(String userId, String userRole, String comment, long time, History.HistoryAction action) {
        this.userId = userId;
        this.userRole = userRole;
        this.comment = comment;
        this.time = time;
        this.action = action;
    }

    public HistoryEntry(String userId, String userRole, String comment, long time, History.HistoryAction action, String version) {
        this.userId = userId;
        this.userRole = userRole;
        this.time = time;
        this.action = action;
        this.registryVersion = version;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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

    public String getRegistryVersion() {
        return registryVersion;
    }

    public void setRegistryVersion(String registryVersion) {
        this.registryVersion = registryVersion;
    }
}
