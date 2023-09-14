package eu.eosc.observatory.dto;

import eu.eosc.observatory.domain.User;

import java.util.Date;
import java.util.List;

public class SurveyAnswerMetadataDTO {

    Date lastUpdate;
    List<User> editors;

    public SurveyAnswerMetadataDTO() {
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public List<User> getEditors() {
        return editors;
    }

    public void setEditors(List<User> editors) {
        this.editors = editors;
    }
}
