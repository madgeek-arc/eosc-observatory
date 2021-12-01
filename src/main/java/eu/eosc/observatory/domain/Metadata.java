package eu.eosc.observatory.domain;

import org.springframework.security.core.Authentication;

import java.util.Date;

public class Metadata {

    private Date creationDate;
    private User createdBy;
    private Date modificationDate;
    private User modifiedBy;

    public Metadata() {}

    public Metadata(Authentication authentication) {
        Date date = new Date();
        this.creationDate = date;
        this.modificationDate = date;
        this.createdBy = User.of(authentication);
        this.modifiedBy = this.createdBy;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(Date modificationDate) {
        this.modificationDate = modificationDate;
    }

    public User getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(User modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
}
