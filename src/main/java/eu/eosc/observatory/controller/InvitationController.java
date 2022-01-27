package eu.eosc.observatory.controller;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.util.Base64URL;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.service.InvitationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("invitation")
public class InvitationController {

    @GetMapping
    @PreAuthorize("isAuthenticated()")

    public ResponseEntity<String> invitationToken(@RequestParam("inviteeEmail") String invitee,
                                                  @RequestParam("inviteeRole") String role,
                                                  @RequestParam("stakeholder") String stakeholderId,
                                                  @ApiIgnore Authentication authentication) throws JOSEException {

//        JWEHeader header = new JWEHeader(JWEAlgorithm.HS256, new EncryptionMethod());
//        JWEObject jweObject = new JWEObject();

        Map<String, Object> invitation = createInvitationObject(User.of(authentication), invitee, role, stakeholderId, 1);

        // Create an HMAC-protected JWS object with some payload
        JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS256),
                new Payload(invitation));

        // We need a 256-bit key for HS256 which must be pre-shared
        byte[] sharedKey = new byte[32];
        new SecureRandom().nextBytes(sharedKey);

        // Create HMAC signer
//        JWSSigner signer = new MACSigner(sharedKey);
        JWSSigner signer = new MACSigner("ThisI$MySecr3tK3yToB3UsedForSigning");
        // Apply the HMAC to the JWS object
        jwsObject.sign(signer);

        // Output in URL-safe format
        System.out.println(jwsObject.serialize());
        return new ResponseEntity<>(jwsObject.serialize(), HttpStatus.OK);
    }

    @GetMapping("accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> acceptInvitation(@RequestParam("invitationToken") String token) throws ParseException, JOSEException {

        String[] parts = token.split("\\.");
        Base64URL base64URL = new Base64URL(token);
        JWSObject jwsObject = new JWSObject(new Base64URL(parts[0]), new Base64URL(parts[1]), new Base64URL(parts[2]));

        JWSVerifier verifier = new MACVerifier("ThisI$MySecr3tK3yToB3UsedForSigning");
        if (!jwsObject.verify(verifier)) {
            throw new RuntimeException("Token has been altered by a third party");
        }

        return new ResponseEntity<>(jwsObject.serialize(), HttpStatus.OK);
    }

    private Map<String, Object> createInvitationObject(User inviter, String invitee, String role, String stakeholderId, int ttlYears) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + ttlYears);

        Map<String, Object> invitation = new LinkedHashMap<>();
        invitation.put("inviter", inviter.getEmail());
        invitation.put("invitee", invitee);
        invitation.put("role", role);
        invitation.put("stakeholder", stakeholderId);
        invitation.put("expiration", calendar.getTime());

        return invitation;
    }
}
