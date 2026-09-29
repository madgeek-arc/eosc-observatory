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

import eu.openaire.observatory.utils.EmailMasking;
import gr.athenarc.messaging.mailer.domain.EmailMessage;
import gr.athenarc.messaging.mailer.service.Mailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Configuration
@Profile("mail-debug")
public class MailDebugConfig {

    private static final Logger logger = LoggerFactory.getLogger(MailDebugConfig.class);

    private final List<EmailMessage> capturedEmails = new CopyOnWriteArrayList<>();

    @Bean
    @Primary
    public Mailer mailer(@Value("${mailer.from}") String systemEmail) {
        return email -> {
            capturedEmails.add(email);
            logger.info(
                    "\n" +
                    "╔══════════════════════════════════════════╗\n" +
                    "║           [MAIL-DEBUG] Email intercepted ║\n" +
                    "╚══════════════════════════════════════════╝\n" +
                    "{{\n" +
                    "  from:    \"{}\",\n" +
                    "  to:      {},\n" +
                    "  bcc:     {},\n" +
                    "  subject: \"{}\",\n" +
                    "  html:    {}\n" +
                    "}}",
                    maskEmail(email.getFrom(), systemEmail),
                    maskEmails(email.getTo(), systemEmail),
                    maskEmails(email.getBcc(), systemEmail),
                    email.getSubject(),
                    email.isHtml()
            );
        };
    }

    private static List<String> maskEmails(List<String> emails, String systemEmail) {
        if (emails == null) {
            return null;
        }
        return emails.stream().map(email -> maskEmail(email, systemEmail)).collect(Collectors.toList());
    }

    private static String maskEmail(String email, String systemEmail) {
        if (email == null || email.equalsIgnoreCase(systemEmail)) {
            return email;
        }
        return EmailMasking.mask(email);
    }

    public List<EmailMessage> getCapturedEmails() {
        return capturedEmails;
    }

    public void clearCapturedEmails() {
        capturedEmails.clear();
    }
}
