package eu.eosc.observatory.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@ConfigurationProperties(prefix = "observatory")
public class ApplicationProperties {

    private Set<Object> admins;

    private String loginRedirect;

    private String logoutRedirect;

    private String jwsSigningSecret;

    public Set<Object> getAdmins() {
        return admins;
    }

    public void setAdmins(Set<Object> admins) {
        this.admins = admins;
    }

    public String getLoginRedirect() {
        return loginRedirect;
    }

    public void setLoginRedirect(String loginRedirect) {
        this.loginRedirect = loginRedirect;
    }

    public String getLogoutRedirect() {
        return logoutRedirect;
    }

    public void setLogoutRedirect(String logoutRedirect) {
        this.logoutRedirect = logoutRedirect;
    }

    public String getJwsSigningSecret() {
        return jwsSigningSecret;
    }

    public void setJwsSigningSecret(String jwsSigningSecret) {
        this.jwsSigningSecret = jwsSigningSecret;
    }
}
