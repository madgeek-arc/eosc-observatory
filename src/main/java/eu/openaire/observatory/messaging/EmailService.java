/**
 * Copyright 2021-2025 OpenAIRE AMKE
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
package eu.openaire.observatory.messaging;

import eu.openaire.observatory.configuration.ApplicationProperties;
import eu.openaire.observatory.domain.NotificationPreferences;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.domain.UserGroup;
import eu.openaire.observatory.service.GroupService;
import eu.openaire.observatory.service.UserService;
import freemarker.template.Configuration;
import gr.athenarc.messaging.domain.Correspondent;
import gr.athenarc.messaging.dto.MessageDTO;
import gr.athenarc.messaging.dto.ThreadDTO;
import gr.athenarc.messaging.mailer.domain.EmailMessage;
import gr.athenarc.messaging.mailer.service.Mailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.StringWriter;
import java.util.*;

@Service
public class EmailService implements EmailOperations {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final Mailer mailClient;
    private final GroupService groupService;
    private final UserService userService;
    private final Configuration configuration;
    private final String emailFrom;
    private final ApplicationProperties applicationProperties;

    public EmailService(Mailer mailClient, GroupService groupService, UserService userService, Configuration configuration, @Value("${mailer.from}") String emailFrom, ApplicationProperties applicationProperties) {
        this.mailClient = mailClient;
        this.groupService = groupService;
        this.userService = userService;
        this.configuration = configuration;
        this.emailFrom = emailFrom;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void sendEmails(ThreadDTO threadDTO) {
        List<EmailMessage> emailMessages = createEmails(threadDTO, emailFrom);
        for (EmailMessage email : emailMessages) {
            mailClient.sendMail(email);
        }
    }

    @Override
    public void sendReminder(ThreadDTO threadDTO) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void sendReport(ThreadDTO threadDTO) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    /**
     * Returns a list of email messages to send when a new message is sent.
     * It creates a separate {@link EmailMessage email message} for the users of each {@link UserGroup}
     * based on the {@link UserGroup#id group id}.
     * @param thread The topic thread.
     * @param sender The email address to be used as sender.
     * @return {@link List}<{@link EmailMessage}>
     */
    List<EmailMessage> createEmails(ThreadDTO thread, String sender) {
        List<EmailMessage> emails = new ArrayList<>();

        MessageDTO messageDTO = thread.getMessages().get(thread.getMessages().size() - 1);
        Map<String, Set<String>> userGroupEmails = getUserGroupEmails(messageDTO.getTo(), true);
        if (StringUtils.hasText(messageDTO.getReplyToMessageId())) {
            MessageDTO replyToMessage = thread.getMessages().get(Integer.parseInt(messageDTO.getReplyToMessageId()));
            userGroupEmails.putAll(getUserGroupEmails(List.of(replyToMessage.getFrom()), false));
        } else {
            userGroupEmails.putAll(getUserGroupEmails(messageDTO.getTo(), false));
        }

        for (Map.Entry<String, Set<String>> entry : userGroupEmails.entrySet()) {
            entry.setValue(filterEmailAddressesBasedOnUserPreferences(entry.getValue()));
            if (StringUtils.hasText(entry.getKey())) {
                emails.add(createEmail(thread, sender, entry.getKey(), entry.getValue(), "emails/inlined-css/message.ftlh"));
            } else {
                emails.add(createEmail(thread, sender, entry.getKey(), entry.getValue(), "emails/inlined-css/external-user_message.ftlh"));
            }
        }

        return emails;
    }

    /**
     * Given a {@link Set} of email addresses, it creates a new collection with the user preferred email addresses.
     * For each email address, it checks whether a {@link User} exists and uses its
     * {@link NotificationPreferences notification preferences} to decide which email address/es
     * to include in the new collection.
     *
     * @param emailAddresses a {@link Set} of email addresses.
     * @return {@link Set}
     */
    Set<String> filterEmailAddressesBasedOnUserPreferences(Set<String> emailAddresses) {
        Set<String> preferredEmailAddresses = new HashSet<>();
        if (emailAddresses != null) {
            for (String email : emailAddresses) {
                User user = null;
                try {
                    user = userService.getUser(email);
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
                if (user != null && user.getSettings() != null && user.getSettings().getNotificationPreferences() != null) {
                    if (!user.getSettings().getNotificationPreferences().isEmailNotifications()) {
                        continue;
                    }
                    if (user.getSettings().getNotificationPreferences().getForwardEmails() != null
                            && !user.getSettings().getNotificationPreferences().getForwardEmails().isEmpty()) {
                        preferredEmailAddresses.addAll(user.getSettings().getNotificationPreferences().getForwardEmails());
                    } else {
                        preferredEmailAddresses.add(email);
                    }
                } else {
                    preferredEmailAddresses.add(email);
                }
            }
        }
        return preferredEmailAddresses;
    }

    /**
     * Creates an EmailMessage based on the provided info using the {@link EmailMessage.EmailBuilder EmailBuilder}.
     * It uses {@link EmailService#createEmailBody} method under the hood to generate the email body.
     *
     * @param thread the thread
     * @param sender The email address to be used as sender.
     * @param group The group id of the users to send the message.
     * @param emails The email addresses of the users to send the message.
     * @param template The path of the message template file.
     * @return {@link EmailMessage}
     */
    EmailMessage createEmail(ThreadDTO thread, String sender, String group, Set<String> emails, String template) {
        return new EmailMessage.EmailBuilder()
                .setFrom(sender)
//                .setCc(ccEmails)
                .setBcc(new ArrayList<>(emails))
                .setSubject(thread.getSubject())
                .setText(createEmailBody(thread, group, template))
                .setHtml(true)
                .build();
    }

    /**
     * Creates an html email body, based on a <a href="https://freemarker.apache.org">FreeMarker</a> template.
     * It contains the user, the subject of the {@link ThreadDTO thread}(conversation) and the body of the message sent.
     *
     * @param threadDTO The {@link ThreadDTO thread}(conversation).
     * @param group The group id of the users the message is referring.
     * @param template The path of a <a href="https://freemarker.apache.org">FreeMarker</a> template file.
     * @return {@link String} The body of the email to send.
     */
    String createEmailBody(ThreadDTO threadDTO, String group, String template) {
        MessageDTO messageDTO = threadDTO.getMessages().get(threadDTO.getMessages().size() - 1);

        if (messageDTO.isAnonymousSender() && StringUtils.hasText(messageDTO.getFrom().getGroupId())) {
            messageDTO.getFrom().setName(groupService.getGroupName(messageDTO.getFrom().getGroupId()));
        }

        StringWriter stringWriter = new StringWriter();
        Map<String, String> data = new HashMap<>();
        data.put("subject", threadDTO.getSubject());
        data.put("content", messageDTO.getBody());
        data.put("user", messageDTO.getFrom().getName());
        data.put("url", String.format("%s/contributions/%s/messages/%s", applicationProperties.getLoginRedirect(), group, threadDTO.getId()));
        try {
            configuration.getTemplate(template).process(data, stringWriter);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        return stringWriter.getBuffer().toString();
    }

    /**
     * Returns a map where each key is a {@link UserGroup#id group id} and its value is the group's users' email.
     *
     * @param correspondents the list of {@link Correspondent correspondents}
     * @param isMember whether the returned results will return only member emails or only non-members.
     * @return {@link Map}<{@link String}, {@link Set}<{@link String}>>
     */
    Map<String, Set<String>> getUserGroupEmails(List<Correspondent> correspondents, boolean isMember) {
        Map<String, Set<String>> userGroupMap = new TreeMap<>();
        for (Correspondent correspondent : correspondents) {
            if (correspondent.getGroupId() != null && isMember) {
                if (userGroupMap.containsKey(correspondent.getGroupId())) {
                    userGroupMap.get(correspondent.getGroupId()).addAll(groupService.getUserIds(correspondent.getGroupId()));
                } else {
                    userGroupMap.put(correspondent.getGroupId(), groupService.getUserIds(correspondent.getGroupId()));
                }
            } else {
                userGroupMap.putIfAbsent("", new HashSet<>());
                userGroupMap.get("").add(correspondent.getEmail());
            }
        }
        return userGroupMap;
    }
}
