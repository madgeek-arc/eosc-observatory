package eu.eosc.observatory.domain;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class Editor implements Serializable {
    private String user;
    private String role;
    private Date updateDate = new Date();

    public Editor() {
        // no-arg constructor
    }

    public Editor(String user, String role) {
        this.user = user;
        this.role = role;
    }

    public String getUser() {
        return user;
    }

    public Editor setUser(String user) {
        this.user = user;
        return this;
    }

    public String getRole() {
        return role;
    }

    public Editor setRole(String role) {
        this.role = role;
        return this;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public Editor setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Editor editor = (Editor) object;
        return Objects.equals(user, editor.user) && Objects.equals(role, editor.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, role);
    }
}
