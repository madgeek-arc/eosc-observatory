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
import eu.openaire.observatory.domain.NotificationPreferences;
import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.User;
import freemarker.template.Configuration;
import gr.athenarc.messaging.mailer.domain.EmailMessage;
import gr.athenarc.messaging.mailer.service.Mailer;
import gr.uoa.di.madgik.catalogue.service.ModelService;
import gr.uoa.di.madgik.catalogue.ui.domain.Model;
import org.springframework.context.annotation.Lazy;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class EmailSurveyService {

    private static final Logger logger = LoggerFactory.getLogger(EmailSurveyService.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy");

    private final Mailer mailClient;
    private final CrudService<Stakeholder> stakeholderCrudService;
    private final ModelService modelService;
    private final SurveyService surveyService;
    private final UserService userService;
    private final Configuration freemarkerConfig;
    private final String emailFrom;
    private final ApplicationProperties applicationProperties;

    public EmailSurveyService(Mailer mailClient,
                              CrudService<Stakeholder> stakeholderCrudService,
                              ModelService modelService,
                              @Lazy SurveyService surveyService,
                              UserService userService,
                              Configuration freemarkerConfig,
                              @Value("${mailer.from}") String emailFrom,
                              ApplicationProperties applicationProperties) {
        this.mailClient = mailClient;
        this.stakeholderCrudService = stakeholderCrudService;
        this.modelService = modelService;
        this.surveyService = surveyService;
        this.userService = userService;
        this.freemarkerConfig = freemarkerConfig;
        this.emailFrom = emailFrom;
        this.applicationProperties = applicationProperties;
    }

    public void notifySurveyStart(String surveyId) {
        try {
            Model survey = modelService.get(surveyId);
            Set<String> recipients = getStakeholderEmailsForSurvey(survey);
            if (recipients.isEmpty()) return;

            Map<String, Object> data = new HashMap<>();
            data.put("surveyName", survey.getName());
            data.put("startDate", survey.getSubmissionStartAt() != null ? DATE_FORMAT.format(survey.getSubmissionStartAt()) : "N/A");
            data.put("url", buildSurveyUrl(surveyId));

            String body = renderTemplate("emails/inlined-css/survey_start.ftlh", data);
            sendBcc("Survey started: " + survey.getName(), body, recipients);
        } catch (Exception e) {
            logger.error("Failed to send survey start notification [surveyId={}]", surveyId, e);
        }
    }

    public void notifySurveyEnd(String surveyId) {
        try {
            Model survey = modelService.get(surveyId);
            Set<String> recipients = getStakeholderEmailsForSurvey(survey);
            if (recipients.isEmpty()) return;

            Map<String, Object> data = new HashMap<>();
            data.put("surveyName", survey.getName());
            data.put("endDate", survey.getSubmissionCloseAt() != null ? DATE_FORMAT.format(survey.getSubmissionCloseAt()) : "N/A");
            data.put("url", buildSurveyUrl(surveyId));

            String body = renderTemplate("emails/inlined-css/survey_end.ftlh", data);
            sendBcc("Survey closed: " + survey.getName(), body, recipients);
        } catch (Exception e) {
            logger.error("Failed to send survey end notification [surveyId={}]", surveyId, e);
        }
    }

    public void notifyDeadlineChange(String surveyId, Date newDeadline) {
        try {
            Model survey = modelService.get(surveyId);
            Set<String> recipients = getStakeholderEmailsForSurvey(survey);
            if (recipients.isEmpty()) return;

            Map<String, Object> data = new HashMap<>();
            data.put("surveyName", survey.getName());
            data.put("newDeadline", newDeadline != null ? DATE_FORMAT.format(newDeadline) : "N/A");
            data.put("url", buildSurveyUrl(surveyId));

            String body = renderTemplate("emails/inlined-css/deadline_change.ftlh", data);
            sendBcc("Deadline updated: " + survey.getName(), body, recipients);
        } catch (Exception e) {
            logger.error("Failed to send deadline change notification [surveyId={}]", surveyId, e);
        }
    }

    public void notifyDeadlineApproaching(String surveyId) {
        try {
            Model survey = modelService.get(surveyId);
            Set<String> recipients = getStakeholderEmailsForSurvey(survey);
            if (recipients.isEmpty()) return;

            Map<String, Object> data = new HashMap<>();
            data.put("surveyName", survey.getName());
            data.put("deadline", survey.getSubmissionCloseAt() != null ? DATE_FORMAT.format(survey.getSubmissionCloseAt()) : "N/A");
            data.put("daysLeft", 7);
            data.put("url", buildSurveyUrl(surveyId));

            String body = renderTemplate("emails/inlined-css/deadline_approaching.ftlh", data);
            sendBcc("Reminder: 7 days left — " + survey.getName(), body, recipients);
        } catch (Exception e) {
            logger.error("Failed to send deadline approaching notification [surveyId={}]", surveyId, e);
        }
    }

    @Scheduled(cron = "${observatory.surveySchedulerCron}")
    public void checkSurveyDates() {
        logger.info("Running daily survey date check");
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysFromNow = today.plusDays(7);
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10_000);
        try {
            List<Model> surveys = surveyService.getByType(filter, null).getResults();
            for (Model model : surveys) {
                try {
                    if (model.getSubmissionStartAt() != null) {
                        LocalDate startDate = toLocalDate(model.getSubmissionStartAt());
                        if (today.equals(startDate)) {
                            notifySurveyStart(model.getId());
                        }
                    }
                    if (model.getSubmissionCloseAt() != null) {
                        LocalDate closeDate = toLocalDate(model.getSubmissionCloseAt());
                        if (today.equals(closeDate)) {
                            notifySurveyEnd(model.getId());
                        } else if (sevenDaysFromNow.equals(closeDate)) {
                            notifyDeadlineApproaching(model.getId());
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing survey date notifications [surveyId={}]", model.getId(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error during daily survey date check", e);
        }
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Set<String> getStakeholderEmailsForSurvey(Model survey) {
        Set<String> emails = new HashSet<>();
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10_000);
        filter.addFilter("type", survey.getType());
        List<Stakeholder> stakeholders = stakeholderCrudService.getAll(filter).getResults();
        for (Stakeholder stakeholder : stakeholders) {
            emails.addAll(stakeholder.getUsers());
        }
        return filterByEmailPreference(emails);
    }

    private Set<String> filterByEmailPreference(Set<String> emailAddresses) {
        Set<String> preferred = new HashSet<>();
        for (String email : emailAddresses) {
            User user = null;
            try {
                user = userService.getUser(email);
            } catch (Exception e) {
                logger.warn("Could not load user preferences for [{}]: {}", email, e.getMessage());
            }
            if (user != null && user.getSettings() != null && user.getSettings().getNotificationPreferences() != null) {
                NotificationPreferences prefs = user.getSettings().getNotificationPreferences();
                if (!prefs.isEmailNotifications()) continue;
                if (!prefs.isSurveyEmailNotifications()) continue;
                if (prefs.getForwardEmails() != null && !prefs.getForwardEmails().isEmpty()) {
                    preferred.addAll(prefs.getForwardEmails());
                } else {
                    preferred.add(email);
                }
            } else {
                preferred.add(email);
            }
        }
        return preferred;
    }

    private String renderTemplate(String templatePath, Map<String, Object> data) {
        StringWriter writer = new StringWriter();
        try {
            freemarkerConfig.getTemplate(templatePath).process(data, writer);
        } catch (Exception e) {
            logger.error("Failed to render template [{}]", templatePath, e);
        }
        return writer.toString();
    }

    private void sendBcc(String subject, String body, Set<String> recipients) {
        if (recipients.isEmpty()) return;
        EmailMessage email = new EmailMessage.EmailBuilder()
                .setFrom(emailFrom)
                .setBcc(new ArrayList<>(recipients))
                .setSubject(subject)
                .setText(body)
                .setHtml(true)
                .build();
        mailClient.sendMail(email);
    }

    private String buildSurveyUrl(String surveyId) {
        return applicationProperties.getLoginRedirect() + "/mySurveys/" + surveyId;
    }
}
