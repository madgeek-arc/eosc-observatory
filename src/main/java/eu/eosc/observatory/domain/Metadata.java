package eu.eosc.observatory.domain;

import org.springframework.security.core.Authentication;

import java.io.Serializable;
import java.util.Date;

public class Metadata implements Serializable {

    private Date creationDate;
    private String createdBy;
    private Date modificationDate;
    private String modifiedBy;

    public Metadata() {
        // no-arg constructor
    }

    public Metadata(Authentication authentication) {
        Date date = new Date();
        this.creationDate = date;
        this.modificationDate = date;
        this.createdBy = authentication != null ? User.getId(authentication) : "unknown";
        this.modifiedBy = this.createdBy;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(Date modificationDate) {
        this.modificationDate = modificationDate;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "creationDate=" + creationDate +
                ", createdBy='" + createdBy + '\'' +
                ", modificationDate=" + modificationDate +
                ", modifiedBy='" + modifiedBy + '\'' +
                '}';
    }
}
