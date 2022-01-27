package eu.eosc.observatory.domain;

import eu.eosc.observatory.service.Identifiable;

import java.util.Date;

public class UserAcceptedPolicy implements Identifiable<String> {

    String id;
    String userId;
    String policyId;
    Date date;

    public UserAcceptedPolicy() {}

    public UserAcceptedPolicy(String userId, String policyId, Date date) {
        this.userId = userId;
        this.policyId = policyId;
        this.date = date;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
