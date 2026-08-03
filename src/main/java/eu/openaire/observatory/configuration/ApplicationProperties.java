/*
 * Copyright 2021-2026 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.openaire.observatory.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "observatory")
public class ApplicationProperties {

    private Set<Object> admins;

    private String baseUrl;

    private String loginRedirect;

    private String logoutRedirect;

    private String jwsSigningSecret;

    /**
     * HMAC key for the pseudonymous subject reference written to the purge report line.
     *
     * <p>Deliberately separate from {@link #jwsSigningSecret}: rotating the signing secret would
     * otherwise silently make every past erasure reference unmatchable. Losing or changing this key
     * has the same effect, so it must be backed up with the same care as the records it indexes.
     */
    private String erasureHashSecret;

    /**
     * Cron expression for the daily survey lifecycle email notification scheduler
     * (survey start, end, deadline approaching). Set to '-' to disable.
     * Example: 0 0 10 * * * (daily at 10:00)
     */
    private String surveySchedulerCron;

    public Set<Object> getAdmins() {
        return admins;
    }

    public void setAdmins(Set<Object> admins) {
        this.admins = admins;
    }

    public String getBaseUrl() { return baseUrl; }

    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getLoginRedirect() { return loginRedirect; }

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

    public String getErasureHashSecret() {
        return erasureHashSecret;
    }

    public void setErasureHashSecret(String erasureHashSecret) {
        this.erasureHashSecret = erasureHashSecret;
    }

    public String getSurveySchedulerCron() {
        return surveySchedulerCron;
    }

    public void setSurveySchedulerCron(String surveySchedulerCron) {
        this.surveySchedulerCron = surveySchedulerCron;
    }
}
