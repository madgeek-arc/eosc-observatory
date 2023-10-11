package eu.eosc.observatory.messaging;

import eu.eosc.observatory.configuration.ApplicationProperties;
import eu.eosc.observatory.service.GroupService;
import freemarker.template.Configuration;
import gr.athenarc.messaging.domain.Correspondent;
import gr.athenarc.messaging.dto.MessageDTO;
import gr.athenarc.messaging.dto.ThreadDTO;
import gr.athenarc.messaging.mailer.client.service.MailClient;
import gr.athenarc.messaging.mailer.domain.EmailMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Service
public class EmailService implements EmailOperations {

    private final MailClient mailClient;
    private final GroupService groupService;
    private final Configuration configuration;
    private final String emailFrom;
    private final ApplicationProperties applicationProperties;

    public EmailService(MailClient mailClient,
                        GroupService groupService,
                        Configuration configuration,
                        @Value("${mailer.from}") String emailFrom,
                        ApplicationProperties applicationProperties) {
        this.mailClient = mailClient;
        this.groupService = groupService;
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

    String createEmailMessageText(ThreadDTO threadDTO, String group, String template) {
        MessageDTO messageDTO = threadDTO.getMessages().get(threadDTO.getMessages().size() - 1);

        if (messageDTO.isAnonymousSender() && !StringUtils.hasText(messageDTO.getFrom().getGroupId())) {
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

        }
        return stringWriter.getBuffer().toString();
    }

    List<EmailMessage> createEmails(ThreadDTO thread, String from) {
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
            if (StringUtils.hasText(entry.getKey())) {
                emails.add(createEmail(thread, from, entry.getKey(), entry.getValue(), "message.ftlh"));
            } else {
                emails.add(createEmail(thread, from, entry.getKey(), entry.getValue(), "external-user_message.ftlh"));
            }
        }

        return emails;
    }

    EmailMessage createEmail(ThreadDTO thread, String from, String group, Set<String> emails, String template) {
        return new EmailMessage.EmailBuilder()
                .setFrom(from)
//                .setCc(ccEmails)
                .setBcc(new ArrayList<>(emails))
                .setSubject(thread.getSubject())
                .setText(createEmailMessageText(thread, group, template))
                .setHtml(true)
                .build();
    }

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
