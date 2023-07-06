package eu.eosc.observatory.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.util.Base64URL;
import eu.eosc.observatory.configuration.ApplicationProperties;
import eu.eosc.observatory.configuration.security.MethodSecurityExpressions;
import eu.eosc.observatory.domain.Invitation;
import eu.eosc.observatory.domain.Roles;
import eu.eosc.observatory.domain.User;
import eu.openminted.registry.core.service.ServiceException;
import gr.athenarc.catalogue.exception.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JWSInvitationService implements InvitationService {

    private static final Logger logger = LoggerFactory.getLogger(JWSInvitationService.class);

    private final StakeholderService stakeholderService;
    private final JWSSigner signer;
    private final JWSVerifier verifier;

    private final MethodSecurityExpressions securityExpressions;

    @Autowired
    public JWSInvitationService(StakeholderService stakeholderService, ApplicationProperties applicationProperties,
                                @Lazy MethodSecurityExpressions securityExpressions) {
        this.stakeholderService = stakeholderService;
        this.securityExpressions = securityExpressions;

        // Create HMAC signer/verifier
        try {
            this.signer = new MACSigner(applicationProperties.getJwsSigningSecret());
            this.verifier = new MACVerifier(applicationProperties.getJwsSigningSecret());
        } catch (JOSEException e) {
            throw new ServiceException(e);
        }
    }

    @Override
    public String createInvitation(User inviter, String inviteeEmail, String role, String stakeholderId) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
        return createInvitation(inviter, inviteeEmail, role, stakeholderId, calendar.getTime());
    }

    @Override
    public String createInvitation(User inviter, String inviteeEmail, String role, String stakeholderId, Date expiration) {
        Map<String, Object> invitation = createInvitationObject(inviter, inviteeEmail, role, stakeholderId, expiration);

        // Create an HMAC-protected JWS object with the invitation as payload
        JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), new Payload(invitation));

        try {
            // Apply the HMAC to the JWS object
            jwsObject.sign(signer);
        } catch (JOSEException e) {
            logger.error(e.getMessage(), e);
        }

        return jwsObject.serialize();
    }

    @Override
    public boolean acceptInvitation(String invitation, Authentication authentication) {
        String[] parts = invitation.split("\\.");
        if (parts.length != 3) {
            throw new ResourceException("Invalid Token", HttpStatus.BAD_REQUEST);
        }

        Invitation invitationObject;
        try {
            JWSObject jwsObject = new JWSObject(new Base64URL(parts[0]), new Base64URL(parts[1]), new Base64URL(parts[2]));
            if (!jwsObject.verify(verifier)) {
                throw new ResourceException("Token has been altered.", HttpStatus.FORBIDDEN);
            }
            invitationObject = getInvitationObject(jwsObject.getPayload().toJSONObject());
            if (new Date().getTime() > invitationObject.getExpiration()) {
                throw new ResourceException("Invitation time has expired.", HttpStatus.FORBIDDEN);
            }
            User authenticatedUser = User.of(authentication);
            if (!authenticatedUser.getId().equals(invitationObject.getInvitee())) {
                throw new ResourceException("Authenticated user email is different than invitee email", HttpStatus.FORBIDDEN);
            }

            if (invitationObject.getRole().equalsIgnoreCase(Roles.Stakeholder.MANAGER.name())
                    && securityExpressions.userIsCoordinatorMemberOfStakeholder(invitationObject.getInviter(), invitationObject.getStakeholderId())) {
                stakeholderService.addManager(invitationObject.getStakeholderId(), invitationObject.getInvitee());
                return true;
            }
            if (invitationObject.getRole().equals(Roles.Stakeholder.CONTRIBUTOR.name())
                    && (securityExpressions.userIsStakeholderManager(invitationObject.getInviter(), invitationObject.getStakeholderId())
                    || securityExpressions.userIsCoordinatorMemberOfStakeholder(invitationObject.getInviter(), invitationObject.getStakeholderId()))) {
                stakeholderService.addContributor(invitationObject.getStakeholderId(), invitationObject.getInvitee());
                return true;
            }
        } catch (ParseException | JOSEException e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    private Map<String, Object> createInvitationObject(User inviter, String invitee, String role, String stakeholderId, Date expiration) {
        Map<String, Object> invitation = new LinkedHashMap<>();
        invitation.put("inviter", inviter.getEmail());
        invitation.put("invitee", invitee);
        invitation.put("role", role);
        invitation.put("stakeholder", stakeholderId);
        invitation.put("expiration", expiration.getTime());

        return invitation;
    }

    private Invitation getInvitationObject(Map<String, Object> jsonObject) {
        Invitation invitation = new Invitation();
        invitation.setInviter(jsonObject.get("inviter").toString());
        invitation.setInvitee(jsonObject.get("invitee").toString());
        invitation.setRole(jsonObject.get("role").toString());
        invitation.setStakeholderId(jsonObject.get("stakeholder").toString());
        invitation.setExpiration((long) jsonObject.get("expiration"));

        return invitation;
    }
}
