package eu.eosc.observatory.dto;

import eu.eosc.observatory.domain.PrivacyPolicy;

public class UserPrivacyPolicyInfo {

    private PrivacyPolicy privacyPolicy;
    private boolean accepted;

    public UserPrivacyPolicyInfo() {
        // no-arg constructor
    }

    public PrivacyPolicy getPrivacyPolicy() {
        return privacyPolicy;
    }

    public void setPrivacyPolicy(PrivacyPolicy privacyPolicy) {
        this.privacyPolicy = privacyPolicy;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
