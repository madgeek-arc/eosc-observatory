package eu.eosc.observatory.domain;

import eu.eosc.observatory.service.Identifiable;

import java.util.Date;

public class PrivacyPolicy implements Identifiable<String> {

    String id;
    String type;
    String filename;
    Date activationDate;

    public PrivacyPolicy() {
        // no-arg constructor
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Date getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(Date activationDate) {
        this.activationDate = activationDate;
    }
}
