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
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import gr.athenarc.messaging.mailer.domain.EmailMessage;
import gr.athenarc.messaging.mailer.service.Mailer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvitationEmailServiceTest {

    private Mailer mailer;
    private InvitationEmailService service;
    private Invitation invitation;

    @BeforeEach
    void setUp() {
        mailer = mock(Mailer.class);
        Configuration freemarker = new Configuration(Configuration.VERSION_2_3_32);
        freemarker.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates"));
        freemarker.setDefaultEncoding("UTF-8");
        ApplicationProperties properties = new ApplicationProperties();
        properties.setLoginRedirect("https://observatory.example.org/home");
        service = new InvitationEmailService(mailer, freemarker, properties, "no-reply@example.org");

        invitation = new Invitation();
        invitation.setInvitee("invitee@example.org");
        invitation.setGroupId("sh-1");
        invitation.setRole("contributor");
        invitation.setExpiresAt(System.currentTimeMillis() + 60_000L);
    }

    @Test
    void sendsToInviteeWithLinkRootedAtUiOrigin() {
        assertTrue(service.send(invitation, "Jane Doe", "Greece", "TOKEN123"));

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(mailer).sendMail(captor.capture());
        EmailMessage email = captor.getValue();
        assertEquals(List.of("invitee@example.org"), email.getTo());
        assertEquals("no-reply@example.org", email.getFrom());
        assertTrue(email.getText().contains("https://observatory.example.org/invitation/accept/TOKEN123"));
        assertTrue(email.getText().contains("Jane Doe"));
        assertTrue(email.getText().contains("Greece"));
    }

    @Test
    void mailerFailureIsReportedNotThrown() {
        doThrow(new RuntimeException("mailer down")).when(mailer).sendMail(any());
        assertFalse(service.send(invitation, "Jane Doe", "Greece", "TOKEN123"));
    }
}
