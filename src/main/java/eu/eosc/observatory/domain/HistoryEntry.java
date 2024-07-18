package eu.eosc.observatory.domain;

import java.io.Serializable;
import java.util.*;

public class HistoryEntry implements Serializable {

    @Deprecated
    String userId;
    @Deprecated
    String userRole;
    List<Editor> editors = new ArrayList<>();
    String comment;
    long time;
    History.HistoryAction action;
    String registryVersion;

    public HistoryEntry() {
        // no-arg constructor
    }

    public HistoryEntry(List<Editor> editors, String comment, long time, History.HistoryAction action) {
        this.editors = editors;
        this.comment = comment;
        this.time = time;
        this.action = action;
    }

    public HistoryEntry(Editor editor, String comment, long time, History.HistoryAction action) {
        this.editors.add(editor);
        this.comment = comment;
        this.time = time;
        this.action = action;
    }

    public HistoryEntry(String userId, String userRole, String comment, long time, History.HistoryAction action) {
        Editor editor = new Editor(userId, userRole);
        this.editors.add(editor);
        this.comment = comment;
        this.time = time;
        this.action = action;
    }

    public HistoryEntry(String userId, String userRole, String comment, long time, History.HistoryAction action, String version) {
        Editor editor = new Editor(userId, userRole);
        this.editors.add(editor);
        this.time = time;
        this.action = action;
        this.registryVersion = version;
    }

    @Deprecated
    public String getUserId() {
        return userId;
    }

    @Deprecated
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Deprecated
    public String getUserRole() {
        return userRole;
    }

    @Deprecated
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public List<Editor> getEditors() {
        return editors;
    }

    public HistoryEntry setEditors(List<Editor> editors) {
        this.editors = editors;
        return this;
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
