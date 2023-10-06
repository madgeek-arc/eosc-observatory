package eu.eosc.observatory.messaging;

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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmailService implements EmailOperations {

    private final MailClient mailClient;
    private final GroupService groupService;
    private final Configuration configuration;
    private final String emailFrom;

    public EmailService(MailClient mailClient,
                        GroupService groupService,
                        Configuration configuration,
                        @Value("${mailer.from}") String emailFrom) {
        this.mailClient = mailClient;
        this.groupService = groupService;
        this.configuration = configuration;
        this.emailFrom = emailFrom;
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

    String createEmailMessageText(ThreadDTO threadDTO, String template) {
        MessageDTO messageDTO = threadDTO.getMessages().get(threadDTO.getMessages().size() - 1);

        if (messageDTO.isAnonymousSender() && !StringUtils.hasText(messageDTO.getFrom().getName())) {
            messageDTO.getFrom().setName(null);
        }

        StringWriter stringWriter = new StringWriter();
        Map<String, String> data = new HashMap<>();
        data.put("subject", threadDTO.getSubject());
        data.put("content", messageDTO.getBody());
        data.put("user", messageDTO.getFrom().getName());
        try {
            configuration.getTemplate(template).process(data, stringWriter);
        } catch (Exception e) {

        }
        return stringWriter.getBuffer().toString();
    }

    List<EmailMessage> createEmails(ThreadDTO thread, String from) {
        List<EmailMessage> emails = new ArrayList<>();

        MessageDTO messageDTO = thread.getMessages().get(thread.getMessages().size() - 1);
        List<String> internalUsers = getUserEmails(messageDTO.getTo(), true);
        List<String> externalUsers;
        if (StringUtils.hasText(messageDTO.getReplyToMessageId())) {
            MessageDTO replyToMessage = thread.getMessages().get(Integer.parseInt(messageDTO.getReplyToMessageId()));
            externalUsers = getUserEmails(List.of(replyToMessage.getFrom()), false);
        } else {
            externalUsers = getUserEmails(messageDTO.getTo(), false);
        }

        emails.add(createEmail(thread, from, internalUsers, "message.ftlh"));
        emails.add(createEmail(thread, from, externalUsers, "external-user_message.ftlh"));

        return emails;
    }

    EmailMessage createEmail(ThreadDTO thread, String from, List<String> emails, String template) {
        return new EmailMessage.EmailBuilder()
                .setFrom(from)
//                .setCc(ccEmails)
                .setBcc(emails)
                .setSubject(thread.getSubject())
                .setText(createEmailMessageText(thread, template))
                .setHtml(true)
                .build();
    }

    List<String> getUserEmails(List<Correspondent> correspondents, boolean isMember) {
        return correspondents
                .stream()
                .map(correspondent -> {
                    if (isMember && correspondent.getGroupId() != null) {
                        return groupService.getUserIds(correspondent.getGroupId());
                    } else if (!isMember && correspondent.getGroupId() == null) {
                        return List.of(correspondent.getEmail());
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
