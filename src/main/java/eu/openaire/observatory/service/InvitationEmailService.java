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

package eu.openaire.observatory.service;

import eu.openaire.observatory.configuration.ApplicationProperties;
import eu.openaire.observatory.domain.Invitation;
import freemarker.template.Configuration;
import gr.athenarc.messaging.mailer.domain.EmailMessage;
import gr.athenarc.messaging.mailer.service.Mailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InvitationEmailService {

    private static final Logger logger = LoggerFactory.getLogger(InvitationEmailService.class);
    private static final String TEMPLATE = "emails/inlined-css/invitation.ftlh";

    private final Mailer mailClient;
    private final Configuration freemarkerConfig;
    private final ApplicationProperties applicationProperties;
    private final String emailFrom;

    public InvitationEmailService(Mailer mailClient,
                                  Configuration freemarkerConfig,
                                  ApplicationProperties applicationProperties,
                                  @Value("${mailer.from}") String emailFrom) {
        this.mailClient = mailClient;
        this.freemarkerConfig = freemarkerConfig;
        this.applicationProperties = applicationProperties;
        this.emailFrom = emailFrom;
    }

    /**
     * Emails the accept link to the invitee.
     *
     * @return whether the email was handed to the mailer; failures are logged and never thrown.
     */
    public boolean send(Invitation invitation, String inviterName, String groupName, String token) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("inviter", inviterName);
            data.put("groupName", groupName);
            data.put("role", invitation.getRole());
            data.put("expiresAt", new SimpleDateFormat("dd MMM yyyy").format(new Date(invitation.getExpiresAt())));
            data.put("url", acceptUrl(token));

            StringWriter writer = new StringWriter();
            freemarkerConfig.getTemplate(TEMPLATE).process(data, writer);

            EmailMessage email = new EmailMessage.EmailBuilder()
                    .setFrom(emailFrom)
                    .setTo(List.of(invitation.getInvitee()))
                    .setSubject("EOSC Observatory - You have been invited to " + groupName)
                    .setText(writer.toString())
                    .setHtml(true)
                    .build();
            mailClient.sendMail(email);
            return true;
        } catch (Exception e) {
            logger.warn("Failed to send invitation email for group [{}]", invitation.getGroupId(), e);
            return false;
        }
    }

    /**
     * The accept link is rooted at the UI origin, since the configured redirect may carry a path.
     */
    private String acceptUrl(String token) {
        URI base = URI.create(applicationProperties.getLoginRedirect());
        String origin = base.getScheme() + "://" + base.getAuthority();
        return origin + "/invitation/accept/" + token;
    }
}
