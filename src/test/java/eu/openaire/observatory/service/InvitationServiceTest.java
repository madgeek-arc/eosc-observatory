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
import eu.openaire.observatory.configuration.security.MethodSecurityExpressions;
import eu.openaire.observatory.domain.Administrator;
import eu.openaire.observatory.domain.Coordinator;
import eu.openaire.observatory.domain.Invitation;
import eu.openaire.observatory.domain.Invitation.Group;
import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.dto.InvitationResultDTO;
import eu.openaire.observatory.utils.OidcTestUtils;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvitationServiceTest {

    private static final String KEY = Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes());
    private static final String OTHER_KEY = Base64.getEncoder().encodeToString("abcdefghijklmnopqrstuvwxyz012345".getBytes());

    @Mock
    private StakeholderService stakeholderService;
    @Mock
    private CoordinatorService coordinatorService;
    @Mock
    private AdministratorService administratorService;
    @Mock
    private UserService userService;
    @Mock
    private InvitationEmailService emailService;
    @Mock
    private MethodSecurityExpressions securityExpressions;

    private InvitationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = newService(KEY);
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setName("Greece");
        when(stakeholderService.get("sh-1")).thenReturn(stakeholder);
        when(emailService.send(any(), anyString(), anyString(), anyString())).thenReturn(true);
    }

    private InvitationServiceImpl newService(String key) {
        ApplicationProperties properties = new ApplicationProperties();
        properties.setInvitationKey(key);
        return new InvitationServiceImpl(stakeholderService, coordinatorService, administratorService, userService,
                emailService, properties, securityExpressions);
    }

    @Test
    void tokenIsAnEncryptedJweWithoutPlaintextEmails() {
        allowManager();
        InvitationResultDTO result = create("manager@example.org", "Invitee@Example.org", "contributor", Group.STAKEHOLDER, "sh-1");

        assertEquals(5, result.token().split("\\.").length);
        String decodedHeaderAndBody = new String(Base64.getUrlDecoder().decode(result.token().split("\\.")[0]))
                + result.token();
        assertFalse(decodedHeaderAndBody.contains("@"));
        assertFalse(decodedHeaderAndBody.contains("invitee"));
        assertTrue(result.emailSent());
    }

    @Test
    void createEmailsTheInviteeAndReportsMailerFailure() {
        allowManager();
        when(emailService.send(any(), anyString(), anyString(), anyString())).thenReturn(false);

        InvitationResultDTO result = create("manager@example.org", "invitee@example.org", "contributor", Group.STAKEHOLDER, "sh-1");

        assertFalse(result.emailSent());
        assertNotNull(result.token());
        verify(emailService).send(any(Invitation.class), anyString(), eq("Greece"), eq(result.token()));
    }

    @Test
    void createRejectsInviterWithoutRights() {
        assertThrows(ResourceException.class, () -> create("nobody@example.org", "invitee@example.org", "contributor", Group.STAKEHOLDER, "sh-1"));
        verifyNoInteractions(emailService);
    }

    @Test
    void createRejectsRoleInvalidForGroup() {
        assertThrows(ResourceException.class, () -> create("a@example.org", "invitee@example.org", "manager", Group.ADMINISTRATOR, "admin-x"));
    }

    @Test
    void acceptContributorInviteAddsMember() {
        allowManager();
        String token = create("manager@example.org", "invitee@example.org", "contributor", Group.STAKEHOLDER, "sh-1").token();

        assertTrue(service.acceptInvitation(token, auth("invitee@example.org")));
        verify(stakeholderService).addMember("sh-1", "invitee@example.org");
        verify(userService).add(any(User.class));
    }

    @Test
    void acceptManagerInviteAddsStakeholderAdmin() {
        when(securityExpressions.userIsCoordinatorOfStakeholder("coordinator@example.org", "sh-1")).thenReturn(true);
        String token = create("coordinator@example.org", "invitee@example.org", "manager", Group.STAKEHOLDER, "sh-1").token();

        assertTrue(service.acceptInvitation(token, auth("invitee@example.org")));
        verify(stakeholderService).addAdmin("sh-1", "invitee@example.org");
    }

    @Test
    void acceptCoordinatorMemberInviteByTypeAdministrator() {
        Coordinator coordinator = new Coordinator();
        coordinator.setType("country");
        when(coordinatorService.get("coord-1")).thenReturn(coordinator);
        when(securityExpressions.userIsAdministratorOfType("admin@example.org", "country")).thenReturn(true);
        String token = create("admin@example.org", "invitee@example.org", "member", Group.COORDINATOR, "coord-1").token();

        assertTrue(service.acceptInvitation(token, auth("invitee@example.org")));
        verify(coordinatorService).addMember("coord-1", "invitee@example.org");
    }

    @Test
    void acceptAdministratorAdminInvite() {
        when(administratorService.get("admin-x")).thenReturn(new Administrator());
        when(securityExpressions.userIsAdministrator("admin@example.org", "admin-x")).thenReturn(true);
        String token = create("admin@example.org", "invitee@example.org", "admin", Group.ADMINISTRATOR, "admin-x").token();

        assertTrue(service.acceptInvitation(token, auth("invitee@example.org")));
        verify(administratorService).addAdmin("admin-x", "invitee@example.org");
    }

    @Test
    void acceptRejectsWrongAuthenticatedUser() {
        allowManager();
        String token = create("manager@example.org", "invitee@example.org", "contributor", Group.STAKEHOLDER, "sh-1").token();

        assertFalse(service.acceptInvitation(token, auth("other@example.org")));
        verifyNoInteractions(userService);
    }

    @Test
    void acceptRejectsExpiredToken() {
        allowManager();
        User inviter = new User();
        inviter.setEmail("manager@example.org");
        String token = service.createInvitation(inviter, "invitee@example.org", "contributor", Group.STAKEHOLDER, "sh-1",
                new Date(System.currentTimeMillis() - 1)).token();

        assertFalse(service.acceptInvitation(token, auth("invitee@example.org")));
    }

    @Test
    void acceptRejectsTamperedAndForeignAndMalformedTokens() {
        allowManager();
        String token = create("manager@example.org", "invitee@example.org", "contributor", Group.STAKEHOLDER, "sh-1").token();
        String[] parts = token.split("\\.");
        char c = parts[3].charAt(0) == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + parts[1] + "." + parts[2] + "." + c + parts[3].substring(1) + "." + parts[4];

        assertFalse(service.acceptInvitation(tampered, auth("invitee@example.org")));
        assertFalse(newService(OTHER_KEY).acceptInvitation(token, auth("invitee@example.org")));
        assertFalse(service.acceptInvitation("not-a-token", auth("invitee@example.org")));
        assertFalse(service.acceptInvitation("", auth("invitee@example.org")));
        assertFalse(service.acceptInvitation(null, auth("invitee@example.org")));
        verifyNoInteractions(userService);
    }

    @Test
    void acceptRejectsWhenInviterLostRights() {
        when(securityExpressions.userIsStakeholderManager("manager@example.org", "sh-1")).thenReturn(true);
        String token = create("manager@example.org", "invitee@example.org", "contributor", Group.STAKEHOLDER, "sh-1").token();
        when(securityExpressions.userIsStakeholderManager("manager@example.org", "sh-1")).thenReturn(false);

        assertFalse(service.acceptInvitation(token, auth("invitee@example.org")));
        verify(stakeholderService, never()).addMember(anyString(), anyString());
    }

    @Test
    void missingOrInvalidKeyFailsFast() {
        assertThrows(IllegalStateException.class, () -> newService(null));
        assertThrows(IllegalStateException.class, () -> newService(""));
        assertThrows(IllegalStateException.class, () -> newService("!!not base64!!"));
        assertThrows(IllegalStateException.class, () -> newService(Base64.getEncoder().encodeToString(new byte[16])));
    }

    private void allowManager() {
        when(securityExpressions.userIsStakeholderManager("manager@example.org", "sh-1")).thenReturn(true);
    }

    private InvitationResultDTO create(String inviterEmail, String invitee, String role, Group group, String groupId) {
        User inviter = new User();
        inviter.setEmail(inviterEmail);
        return service.createInvitation(inviter, invitee, role, group, groupId);
    }

    private UsernamePasswordAuthenticationToken auth(String email) {
        return OidcTestUtils.oidcAuthentication(email);
    }
}
