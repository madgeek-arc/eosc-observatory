package eu.eosc.observatory.domain;

import java.util.List;

public class NotificationPreferences {

    private boolean emailNotifications;
    private List<String> forwardEmails;

    public NotificationPreferences() {
    }

    public boolean isEmailNotifications() {
        return emailNotifications;
    }

    public NotificationPreferences setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
        return this;
    }

    public List<String> getForwardEmails() {
        return forwardEmails;
    }

    public NotificationPreferences setForwardEmails(List<String> forwardEmails) {
        this.forwardEmails = forwardEmails;
        return this;
    }


}
