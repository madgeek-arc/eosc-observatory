package eu.eosc.observatory.dto;

import eu.eosc.observatory.domain.User;

import java.util.Collection;
import java.util.Date;

public class SurveyAnswerMetadataDTO {

    Date lastUpdate;
    Collection<User> editors;

    public SurveyAnswerMetadataDTO() {
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Collection<User> getEditors() {
        return editors;
    }

    public void setEditors(Collection<User> editors) {
        this.editors = editors;
    }
}
