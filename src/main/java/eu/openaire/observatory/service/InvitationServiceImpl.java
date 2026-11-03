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

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import eu.openaire.observatory.configuration.ApplicationProperties;
import eu.openaire.observatory.configuration.security.MethodSecurityExpressions;
import eu.openaire.observatory.domain.Coordinator;
import eu.openaire.observatory.domain.Invitation;
import eu.openaire.observatory.domain.Invitation.Group;
import eu.openaire.observatory.domain.Roles;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.domain.UserGroup;
import eu.openaire.observatory.dto.InvitationResultDTO;
import gr.uoa.di.madgik.registry.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Invitations are stateless: everything is carried in a JWE (direct AES-256-GCM) that only this
 * service can read or forge.
 */
@Service
public class InvitationServiceImpl implements InvitationService {

    private static final Logger logger = LoggerFactory.getLogger(InvitationServiceImpl.class);

    private static final String ROLE_MEMBER = "member";
    private static final String ROLE_ADMIN = "admin";
    private static final int KEY_BYTES = 32;

    private final StakeholderService stakeholderService;
    private final CoordinatorService coordinatorService;
    private final AdministratorService administratorService;
    private final UserService userService;
    private final InvitationEmailService emailService;
    private final MethodSecurityExpressions securityExpressions;
    private final Duration ttl;
    private final byte[] key;

    public InvitationServiceImpl(@Lazy StakeholderService stakeholderService,
                                 @Lazy CoordinatorService coordinatorService,
                                 @Lazy AdministratorService administratorService,
                                 @Lazy UserService userService,
                                 InvitationEmailService emailService,
                                 ApplicationProperties applicationProperties,
                                 @Lazy MethodSecurityExpressions securityExpressions) {
        this.stakeholderService = stakeholderService;
        this.coordinatorService = coordinatorService;
        this.administratorService = administratorService;
        this.userService = userService;
        this.emailService = emailService;
        this.securityExpressions = securityExpressions;
        this.ttl = applicationProperties.getInvitationTtl();
        this.key = decodeKey(applicationProperties.getInvitationKey());
    }

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] decodeKey(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("observatory.invitationKey is not set; generate one with 'openssl rand -base64 32'");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(encoded.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("observatory.invitationKey is not valid base64", e);
        }
        if (key.length != KEY_BYTES) {
            throw new IllegalStateException("observatory.invitationKey must decode to " + KEY_BYTES + " bytes, got " + key.length);
        }
        return key;
    }

    @Override
    public InvitationResultDTO createInvitation(User inviter, String inviteeEmail, String role, Group group, String groupId) {
        return createInvitation(inviter, inviteeEmail, role, group, groupId, new Date(System.currentTimeMillis() + ttl.toMillis()));
    }

    @Override
    public InvitationResultDTO createInvitation(User inviter, String inviteeEmail, String role, Group group, String groupId, Date expiration) {
        Invitation invitation = new Invitation();
        invitation.setInviter(normalize(inviter.getEmail()));
        invitation.setInvitee(normalize(inviteeEmail));
        invitation.setGroup(group);
        invitation.setGroupId(groupId);
        invitation.setRole(normalizeRole(group, role));
        invitation.setExpiresAt(expiration.getTime());

        UserGroup target = getGroup(group, groupId);
        if (!inviterMayInvite(invitation)) {
            throw new ResourceException("You are not allowed to invite users to this group.", HttpStatus.FORBIDDEN);
        }

        String token = encrypt(invitation);
        String inviterName = inviter.getName() == null || inviter.getName().isBlank() ? invitation.getInviter() : inviter.getName();
        boolean emailSent = emailService.send(invitation, inviterName, target.getName() != null ? target.getName() : groupId, token);
        return new InvitationResultDTO(token, emailSent);
    }

    @Override
    public boolean acceptInvitation(String token, Authentication authentication) {
        Invitation invitation = decrypt(token);
        if (invitation == null || System.currentTimeMillis() > invitation.getExpiresAt()) {
            return false;
        }
        User authenticatedUser = User.of(authentication);
        if (!normalize(authenticatedUser.getId()).equals(invitation.getInvitee())) {
            return false;
        }
        if (!inviterMayInvite(invitation)) {
            return false;
        }

        UserGroupService groupService = groupService(invitation.getGroup());
        if (isAdminRole(invitation.getGroup(), invitation.getRole())) {
            groupService.addAdmin(invitation.getGroupId(), invitation.getInvitee());
        } else {
            groupService.addMember(invitation.getGroupId(), invitation.getInvitee());
        }
        try {
            userService.add(authenticatedUser);
        } catch (ResourceAlreadyExistsException e) {
            // the user is already registered
        }
        return true;
    }

    private String encrypt(Invitation invitation) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("inviter", invitation.getInviter());
        claims.put("invitee", invitation.getInvitee());
        claims.put("group", invitation.getGroup().getKey());
        claims.put("groupId", invitation.getGroupId());
        claims.put("role", invitation.getRole());
        claims.put("exp", invitation.getExpiresAt());
        claims.put("iat", System.currentTimeMillis());
        claims.put("jti", UUID.randomUUID().toString());
        try {
            JWEObject jwe = new JWEObject(new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM), new Payload(claims));
            jwe.encrypt(new DirectEncrypter(key));
            return jwe.serialize();
        } catch (JOSEException e) {
            throw new ServiceException(e);
        }
    }

    /**
     * @return the invitation, or null when the token is malformed, tampered, or not produced with this key.
     */
    private Invitation decrypt(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            JWEObject jwe = JWEObject.parse(token);
            if (!JWEAlgorithm.DIR.equals(jwe.getHeader().getAlgorithm())
                    || !EncryptionMethod.A256GCM.equals(jwe.getHeader().getEncryptionMethod())) {
                return null;
            }
            jwe.decrypt(new DirectDecrypter(key));
            Map<String, Object> claims = jwe.getPayload().toJSONObject();

            Invitation invitation = new Invitation();
            invitation.setInviter(claims.get("inviter").toString());
            invitation.setInvitee(claims.get("invitee").toString());
            invitation.setGroup(Group.fromString(claims.get("group").toString()));
            invitation.setGroupId(claims.get("groupId").toString());
            invitation.setRole(claims.get("role").toString());
            invitation.setExpiresAt(((Number) claims.get("exp")).longValue());
            return invitation;
        } catch (ParseException | JOSEException | RuntimeException e) {
            logger.debug("Rejected invitation token: {}", e.getMessage());
            return null;
        }
    }

    private UserGroupService groupService(Group group) {
        return switch (group) {
            case STAKEHOLDER -> stakeholderService;
            case COORDINATOR -> coordinatorService;
            case ADMINISTRATOR -> administratorService;
        };
    }

    private UserGroup getGroup(Group group, String groupId) {
        return switch (group) {
            case STAKEHOLDER -> stakeholderService.get(groupId);
            case COORDINATOR -> coordinatorService.get(groupId);
            case ADMINISTRATOR -> administratorService.get(groupId);
        };
    }

    private static String normalizeRole(Group group, String role) {
        String r = role == null ? "" : role.trim().toLowerCase();
        boolean valid = switch (group) {
            case STAKEHOLDER -> r.equals(Roles.Stakeholder.MANAGER.getRoleName())
                    || r.equals(Roles.Stakeholder.CONTRIBUTOR.getRoleName());
            case COORDINATOR, ADMINISTRATOR -> r.equals(ROLE_MEMBER) || r.equals(ROLE_ADMIN);
        };
        if (!valid) {
            throw new ResourceException("Invalid role '" + role + "' for group " + group.getKey(), HttpStatus.BAD_REQUEST);
        }
        return r;
    }

    private static boolean isAdminRole(Group group, String role) {
        return group == Group.STAKEHOLDER
                ? role.equals(Roles.Stakeholder.MANAGER.getRoleName())
                : role.equals(ROLE_ADMIN);
    }

    /**
     * Whether the inviter currently holds the right to grant the invitation's role in its group.
     */
    private boolean inviterMayInvite(Invitation invitation) {
        String inviterId = invitation.getInviter();
        String groupId = invitation.getGroupId();
        Group group = invitation.getGroup();
        try {
            return switch (group) {
                case STAKEHOLDER -> isAdminRole(group, invitation.getRole())
                        ? securityExpressions.userIsCoordinatorOfStakeholder(inviterId, groupId)
                        : securityExpressions.userIsStakeholderManager(inviterId, groupId)
                                || securityExpressions.userIsCoordinatorOfStakeholder(inviterId, groupId);
                case COORDINATOR -> {
                    Coordinator coordinator = coordinatorService.get(groupId);
                    yield securityExpressions.userIsAdministratorOfType(inviterId, coordinator.getType())
                            || (!isAdminRole(group, invitation.getRole()) && securityExpressions.userIsCoordinator(inviterId, groupId));
                }
                case ADMINISTRATOR -> securityExpressions.userIsAdministrator(inviterId, groupId);
            };
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }
}
