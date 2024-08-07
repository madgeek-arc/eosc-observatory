package eu.eosc.observatory.domain;

import java.io.Serializable;
import java.util.Date;

public class Revision implements Serializable {

    private String field;
    private Object value;
    private Action action = new Action();
    private String sessionId;
    private Date date = new Date();

    public Revision() {
        // no-arg constructor
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Action getAction() {
        return action;
    }

    public Revision setAction(Action action) {
        this.action = action;
        return this;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Revision setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public Date getDate() {
        return date;
    }

    public Revision setDate(Date date) {
        this.date = date;
        return this;
    }
}
