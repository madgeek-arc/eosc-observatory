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

import gr.athenarc.messaging.mailer.domain.EmailMessage;
import gr.athenarc.messaging.mailer.service.Mailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Configuration
@Profile("mail-debug")
public class MailDebugConfig {

    private static final Logger logger = LoggerFactory.getLogger(MailDebugConfig.class);

    private final List<EmailMessage> capturedEmails = new CopyOnWriteArrayList<>();

    @Bean
    @Primary
    public Mailer mailer() {
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
                    email.getFrom(),
                    email.getTo(),
                    email.getBcc(),
                    email.getSubject(),
                    email.isHtml()
            );
        };
    }

    public List<EmailMessage> getCapturedEmails() {
        return capturedEmails;
    }

    public void clearCapturedEmails() {
        capturedEmails.clear();
    }
}
