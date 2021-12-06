package eu.eosc.observatory.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@ConfigurationProperties(prefix = "observatory")
public class ApplicationProperties {

    private Set<Object> admins;

    public Set<Object> getAdmins() {
        return admins;
    }

    public void setAdmins(Set<Object> admins) {
        this.admins = admins;
    }
}
