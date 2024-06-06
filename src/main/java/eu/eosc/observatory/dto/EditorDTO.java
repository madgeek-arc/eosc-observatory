package eu.eosc.observatory.dto;

import java.util.Date;

public class EditorDTO {

    private String email;
    private String fullname;
    private String role;
    private Date updateDate;

    public EditorDTO() {
    }

    public EditorDTO(String email, String role, Date updateDate) {
        this.email = email;
        this.role = role;
        this.updateDate = updateDate;
    }

    public EditorDTO(String email, String fullname, String role, Date updateDate) {
        this.email = email;
        this.fullname = fullname;
        this.role = role;
        this.updateDate = updateDate;
    }

    public String getEmail() {
        return email;
    }

    public EditorDTO setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getFullname() {
        return fullname;
    }

    public EditorDTO setFullname(String fullname) {
        this.fullname = fullname;
        return this;
    }

    public String getRole() {
        return role;
    }

    public EditorDTO setRole(String role) {
        this.role = role;
        return this;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public EditorDTO setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
        return this;
    }
}
