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

package eu.openaire.observatory.commenting;

import eu.openaire.observatory.commenting.domain.CommentMessage;
import eu.openaire.observatory.configuration.ApplicationProperties;
import eu.openaire.observatory.domain.NotificationPreferences;
import eu.openaire.observatory.domain.Settings;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.service.SurveyAnswerCrudService;
import eu.openaire.observatory.service.UserService;
import freemarker.template.Configuration;
import gr.athenarc.messaging.mailer.domain.EmailMessage;
import gr.athenarc.messaging.mailer.service.Mailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(CommentNotificationService.class);
    private static final String TEMPLATE = "emails/comment_notification_observatory.ftlh";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm 'UTC'").withZone(ZoneId.of("UTC"));

    private final UserService userService;
    private final Configuration freemarkerConfig;
    private final Mailer mailClient;
    private final String emailFrom;
    private final SurveyAnswerCrudService surveyAnswerCrudService;
    private final ApplicationProperties applicationProperties;

    public CommentNotificationService(UserService userService,
                                      Configuration freemarkerConfig,
                                      Mailer mailClient,
                                      @Value("${mailer.from}") String emailFrom,
                                      SurveyAnswerCrudService surveyAnswerCrudService,
                                      ApplicationProperties applicationProperties) {
        this.userService = userService;
        this.freemarkerConfig = freemarkerConfig;
        this.mailClient = mailClient;
        this.emailFrom = emailFrom;
        this.surveyAnswerCrudService = surveyAnswerCrudService;
        this.applicationProperties = applicationProperties;
    }

    public void notifyMentions(CommentMessage message) {
        if (message.getMentions() == null || message.getMentions().isEmpty()) {
            return;
        }

        String authorEmail = message.getAuthorId();
        User author = null;
        try {
            author = userService.getUser(authorEmail);
        } catch (Exception e) {
            logger.warn("Could not fetch author for mention notification: {}", e.getMessage());
        }
        String authorName = author != null && author.getFullname() != null ? author.getFullname() : authorEmail;
        String fieldId = message.getComment() != null ? message.getComment().getFieldId() : "";
        String baseUrl = applicationProperties.getBaseUrl();
        String stakeholderId = "";
        String surveyId = "";
        try {
            String targetId = message.getComment().getTarget().getId();
            SurveyAnswer surveyAnswer = surveyAnswerCrudService.get(targetId);
            stakeholderId = surveyAnswer.getStakeholderId();
            surveyId = surveyAnswer.getSurveyId();
        } catch (Exception e) {
            logger.warn("Could not fetch survey answer for mention notification: {}", e.getMessage());
        }

        for (var mention : message.getMentions()) {
            String mentionedEmail = mention.getId().getUserId();
            try {
                sendMentionEmail(mentionedEmail, authorName, authorEmail, message.getBody(), fieldId, baseUrl, stakeholderId, surveyId, message);
            } catch (Exception e) {
                logger.warn("Failed to send mention notification to {}: {}", mentionedEmail, e.getMessage());
            }
        }
    }

    private void sendMentionEmail(String toEmail, String fromName, String fromEmail,
                                   String body, String fieldId, String baseUrl, String stakeholderId, String surveyId, CommentMessage message) {
        User recipient = null;
        try {
            recipient = userService.getUser(toEmail);
        } catch (Exception e) {
            logger.warn("Could not fetch recipient {} for mention notification: {}", toEmail, e.getMessage());
        }

        if (!shouldSendEmail(recipient)) {
            return;
        }

        String toName = recipient != null && recipient.getFullname() != null ? recipient.getFullname() : toEmail;
        List<String> sendTo = resolveEmailAddresses(recipient, toEmail);

        Map<String, Object> data = new HashMap<>();
        data.put("to", Map.of("name", toName));
        data.put("from", Map.of("name", fromName, "email", fromEmail));
        data.put("body", body);
        data.put("chapter", Map.of("name", fieldId));
        data.put("baseUrl", baseUrl);
        data.put("stakeholderId", stakeholderId);
        data.put("surveyId", surveyId);
        data.put("date", message.getCreatedAt() != null ? DATE_FORMATTER.format(message.getCreatedAt()) : "");

        StringWriter writer = new StringWriter();
        try {
            freemarkerConfig.getTemplate(TEMPLATE).process(data, writer);
        } catch (Exception e) {
            logger.error("Failed to render mention notification template: {}", e.getMessage(), e);
            return;
        }

        EmailMessage email = new EmailMessage.EmailBuilder()
                .setFrom(emailFrom)
                .setBcc(sendTo)
                .setSubject(fromName + " mentioned you in a comment on EOSC Observatory")
                .setText(writer.toString())
                .setHtml(true)
                .build();

        mailClient.sendMail(email);
    }

    private boolean shouldSendEmail(User user) {
        if (user == null) {
            return true;
        }
        Settings settings = user.getSettings();
        if (settings == null) {
            return true;
        }
        NotificationPreferences prefs = settings.getNotificationPreferences();
        if (prefs == null) {
            return true;
        }
        if (!prefs.isEmailNotifications()) {
            return false;
        }
        return prefs.isMentionEmailNotifications();
    }

    private List<String> resolveEmailAddresses(User user, String fallbackEmail) {
        if (user != null
                && user.getSettings() != null
                && user.getSettings().getNotificationPreferences() != null
                && user.getSettings().getNotificationPreferences().getForwardEmails() != null
                && !user.getSettings().getNotificationPreferences().getForwardEmails().isEmpty()) {
            return user.getSettings().getNotificationPreferences().getForwardEmails();
        }
        return List.of(fallbackEmail);
    }
}
