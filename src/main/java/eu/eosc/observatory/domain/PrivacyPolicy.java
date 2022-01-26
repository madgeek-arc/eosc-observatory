package eu.eosc.observatory.domain;

import eu.eosc.observatory.service.Identifiable;

import java.util.Date;

public class PrivacyPolicy implements Identifiable<String> {

    String doi;
    String type;
    Date activationDate;

    public PrivacyPolicy() {}

    @Override
    public String getId() {
        return doi;
    }

    @Override
    public void setId(String id) {
        this.doi = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(Date activationDate) {
        this.activationDate = activationDate;
    }
}
