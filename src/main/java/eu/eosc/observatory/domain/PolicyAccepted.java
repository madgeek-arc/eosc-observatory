package eu.eosc.observatory.domain;

import java.util.Date;

public class PolicyAccepted {
    String id;
    Date acceptedDate;

    public PolicyAccepted() {}

    public PolicyAccepted(String id, Date acceptedDate) {
        this.id = id;
        this.acceptedDate = acceptedDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getAcceptedDate() {
        return acceptedDate;
    }

    public void setAcceptedDate(Date acceptedDate) {
        this.acceptedDate = acceptedDate;
    }
}
