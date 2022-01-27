package eu.eosc.observatory.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.util.Base64URL;
import eu.eosc.observatory.configuration.ApplicationProperties;
import eu.eosc.observatory.domain.User;
import eu.openminted.registry.core.service.ServiceException;
import gr.athenarc.catalogue.exception.ResourceException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JWSInvitationService implements InvitationService {

    private static final Logger logger = LogManager.getLogger(JWSInvitationService.class);

    private final StakeholderService stakeholderService;
    private final JWSSigner signer;
    private final JWSVerifier verifier;

    @Autowired
    public JWSInvitationService(StakeholderService stakeholderService, ApplicationProperties applicationProperties) {
        this.stakeholderService = stakeholderService;

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
            logger.error(e);
        }

        return jwsObject.serialize();
    }

    @Override
    public boolean acceptInvitation(String invitation) {
        String[] parts = invitation.split("\\.");
        if (parts.length != 3) {
            throw new ResourceException("Invalid Token", HttpStatus.BAD_REQUEST);
        }

        try {
            JWSObject jwsObject = new JWSObject(new Base64URL(parts[0]), new Base64URL(parts[1]), new Base64URL(parts[2]));
            if (!jwsObject.verify(verifier)) {
                throw new RuntimeException("Token has been altered by a third party");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (JOSEException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Map<String, Object> createInvitationObject(User inviter, String invitee, String role, String stakeholderId, Date expiration) {
        Map<String, Object> invitation = new LinkedHashMap<>();
        invitation.put("inviter", inviter.getEmail());
        invitation.put("invitee", invitee);
        invitation.put("role", role);
        invitation.put("stakeholder", stakeholderId);
        invitation.put("expiration", expiration);

        return invitation;
    }
}
