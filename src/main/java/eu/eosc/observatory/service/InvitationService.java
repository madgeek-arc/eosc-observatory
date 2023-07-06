package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.User;
import org.springframework.security.core.Authentication;

import java.util.Date;

public interface InvitationService {

    String createInvitation(User inviter, String inviteeEmail, String role, String stakeholderId);

    String createInvitation(User inviter, String inviteeEmail, String role, String stakeholderId, Date expiration);

    boolean acceptInvitation(String invitation, Authentication authentication);

}
