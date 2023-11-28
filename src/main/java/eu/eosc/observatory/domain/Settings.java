package eu.eosc.observatory.domain;

public class Settings {

    private NotificationPreferences notificationPreferences = new NotificationPreferences();

    public Settings() {
    }

    public NotificationPreferences getNotificationPreferences() {
        return notificationPreferences;
    }

    public Settings setNotificationPreferences(NotificationPreferences notificationPreferences) {
        this.notificationPreferences = notificationPreferences;
        return this;
    }
}
